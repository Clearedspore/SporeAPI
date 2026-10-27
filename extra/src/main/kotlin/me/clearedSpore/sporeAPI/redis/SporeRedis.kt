
package me.clearedSpore.sporeAPI.redis

import com.google.gson.Gson
import kotlinx.coroutines.Job
import me.clearedSpore.sporeAPI.annotation.RegisterPacket
import me.clearedSpore.sporeAPI.annotation.RegisterPacketListener
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.SporeDebug
import me.clearedSpore.sporeAPI.debug.blockingIo
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.debug.runDebug
import me.clearedSpore.sporeAPI.scan.SporeScanner
import me.clearedSpore.sporeAPI.util.Logger
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class PacketSubscription internal constructor(private val onUnregister: () -> Unit) {

    @Volatile
    var isActive: Boolean = true
        private set

    fun unregister() {
        if (!isActive) return
        isActive = false
        onUnregister()
    }
}

object SporeRedis {

    private const val MAX_RECONNECT_DELAY_MILLIS = 30_000L

    val instanceId: String = UUID.randomUUID().toString()

    private val gson = Gson()

    private val packetTypes = ConcurrentHashMap<String, Class<out Packet>>()
    private val handlers = ConcurrentHashMap<Class<out Packet>, CopyOnWriteArrayList<Handler>>()

    private val sent = AtomicLong()
    private val received = AtomicLong()

    @Volatile
    private var settings: RedisSettings? = null

    @Volatile
    private var pool: JedisPool? = null

    @Volatile
    private var subscriber: Subscriber? = null

    @Volatile
    private var subscriberThread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    var isConnected: Boolean = false
        private set

    private class Handler(val async: Boolean, val invoke: (Packet) -> Unit)

    fun connect(settings: RedisSettings, mainClass: Class<*>? = null) {
        check(!running) { "SporeRedis is already connected, call shutdown() first" }

        this.settings = settings
        pool = createPool(settings)
        running = true

        SporeDebug.addStatus("Redis") {
            val state = if (isConnected) "<s_green>connected" else if (running) "<s_red>reconnecting" else "<gray>stopped"
            "$state <gray>(${settings.channel}, <white>${sent.get()} <gray>sent, <white>${received.get()} <gray>received)"
        }

        if (mainClass != null) scan(mainClass)

        subscriberThread = Thread(::runSubscriber, "SporeRedis-Subscriber").apply {
            isDaemon = true
            start()
        }
    }

    fun scan(mainClass: Class<*>) {
        var packets = 0
        SporeScanner.annotatedWith(mainClass, RegisterPacket::class.java).forEach { clazz ->
            if (!Packet::class.java.isAssignableFrom(clazz)) {
                Logger.warn("${clazz.simpleName} is annotated with @RegisterPacket but does not extend Packet")
                return@forEach
            }

            @Suppress("UNCHECKED_CAST")
            register(clazz as Class<out Packet>)
            packets++
        }

        var listeners = 0
        SporeScanner.annotatedWith(mainClass, RegisterPacketListener::class.java).forEach { clazz ->
            try {
                val async = clazz.getAnnotation(RegisterPacketListener::class.java).async
                listen(SporeScanner.instantiate(clazz), async)
                listeners++
            } catch (exception: Exception) {
                IncidentReporter.report("redis.listener.register", exception, details = mapOf("class" to clazz.name))
            }
        }

        Logger.info("Redis: registered $packets packet(s) and $listeners packet listener(s)")
    }

    fun register(packetClass: Class<out Packet>) {
        if (packetTypes.putIfAbsent(packetClass.name, packetClass) != null) return
        if (!overridesOnReceive(packetClass)) return

        val async = packetClass.getAnnotation(RegisterPacket::class.java)?.async ?: false
        handlers.computeIfAbsent(packetClass) { CopyOnWriteArrayList() } += Handler(async) { packet ->
            if (packet.javaClass == packetClass) packet.onReceive()
        }
    }

    inline fun <reified T : Packet> register() = register(T::class.java)

    fun <T : Packet> onReceive(type: Class<T>, async: Boolean = false, handler: (T) -> Unit): PacketSubscription {
        register(type)

        val entry = Handler(async) { packet -> handler(type.cast(packet)) }
        handlers.computeIfAbsent(type) { CopyOnWriteArrayList() } += entry

        return PacketSubscription { handlers[type]?.remove(entry) }
    }

    inline fun <reified T : Packet> onReceive(async: Boolean = false, noinline handler: (T) -> Unit): PacketSubscription =
        onReceive(T::class.java, async, handler)

    fun listen(listener: Any, async: Boolean = false): Int {
        val methods = listener.javaClass.declaredMethods.filter { method ->
            method.name == "onReceive" &&
                method.parameterCount == 1 &&
                Packet::class.java.isAssignableFrom(method.parameterTypes[0])
        }

        if (methods.isEmpty()) {
            Logger.warn("${listener.javaClass.simpleName} has no onReceive(packet) methods")
        }

        methods.forEach { method ->
            method.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            onReceive(method.parameterTypes[0] as Class<Packet>, async) { packet ->
                try {
                    method.invoke(listener, packet)
                } catch (exception: InvocationTargetException) {
                    throw exception.targetException
                }
            }
        }

        return methods.size
    }

    fun send(packet: Packet) {
        val settings = settings ?: error("SporeRedis.connect(...) has not been called")
        val json = gson.toJson(PacketEnvelope(packet.javaClass.name, gson.toJsonTree(packet), instanceId))

        blockingIo("redis.publish") {
            publisher().resource.use { jedis -> jedis.publish(settings.channel, json) }
        }

        sent.incrementAndGet()
    }

    fun sendAsync(packet: Packet): Job = SporeCoroutines.launchAsync {
        try {
            send(packet)
        } catch (exception: Exception) {
            IncidentReporter.report("redis.send", exception, details = mapOf("packet" to packet.javaClass.simpleName))
        }
    }

    fun shutdown() {
        if (!running) return
        running = false
        isConnected = false

        subscriber?.let { if (it.isSubscribed) runCatching { it.unsubscribe() } }
        subscriber = null

        subscriberThread?.interrupt()
        subscriberThread = null

        pool?.close()
        pool = null
    }

    inline fun <T> connection(block: (Jedis) -> T): T =
        blockingIo("redis.command") { publisher().resource.use(block) }

    @PublishedApi
    internal fun publisher(): JedisPool = pool ?: error("SporeRedis is not connected")

    private fun createPool(settings: RedisSettings): JedisPool {
        val config = JedisPoolConfig().apply {
            maxTotal = 8
            maxIdle = 4
            setMaxWait(Duration.ofMillis(settings.timeoutMillis.toLong()))
            testOnBorrow = true
        }

        return JedisPool(
            config,
            settings.host,
            settings.port,
            settings.timeoutMillis,
            settings.username.ifBlank { null },
            settings.password.ifBlank { null }
        )
    }

    private fun createConnection(settings: RedisSettings): Jedis =
        Jedis(settings.host, settings.port, settings.timeoutMillis).apply {
            when {
                settings.username.isNotBlank() -> auth(settings.username, settings.password)
                settings.password.isNotBlank() -> auth(settings.password)
            }
            ping()
        }

    private fun runSubscriber() {
        var delay = 1_000L

        while (running) {
            val settings = settings ?: return

            try {
                createConnection(settings).use { connection ->
                    val listener = Subscriber()
                    subscriber = listener

                    delay = 1_000L
                    connection.subscribe(listener, settings.channel)
                }
            } catch (exception: Exception) {
                if (!running) break

                isConnected = false
                IncidentReporter.report(
                    "redis.subscribe", exception, Severity.WARNING,
                    mapOf("address" to settings.address, "retryIn" to "${delay}ms")
                )
            }

            if (!running) break

            try {
                Thread.sleep(delay)
            } catch (_: InterruptedException) {
                if (!running) break
            }

            delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MILLIS)
        }

        isConnected = false
    }

    private fun receive(message: String) {
        val settings = settings ?: return

        val envelope = runDebug("redis.receive", Severity.WARNING) {
            gson.fromJson(message, PacketEnvelope::class.java)
        } ?: return

        if (!settings.receiveOwnPackets && envelope.origin == instanceId) return

        val type = packetTypes[envelope.type] ?: return

        val packet = runDebug("redis.receive", Severity.WARNING, mapOf("packet" to type.simpleName)) {
            gson.fromJson(envelope.data, type)
        } ?: return

        received.incrementAndGet()

        val targets = handlersFor(type)
        if (targets.isEmpty()) return

        val operation = "redis.packet.${type.simpleName}"
        val (async, sync) = targets.partition { it.async }

        if (sync.isNotEmpty()) {
            SporeCoroutines.launch { sync.forEach { handler -> runDebug(operation) { handler.invoke(packet) } } }
        }
        if (async.isNotEmpty()) {
            SporeCoroutines.launchAsync { async.forEach { handler -> runDebug(operation) { handler.invoke(packet) } } }
        }
    }

    private fun overridesOnReceive(packetClass: Class<*>): Boolean =
        generateSequence(packetClass) { it.superclass }
            .takeWhile { it != Packet::class.java }
            .any { clazz -> clazz.declaredMethods.any { it.name == "onReceive" && it.parameterCount == 0 } }

    private fun handlersFor(type: Class<*>): List<Handler> =
        handlers.entries.filter { it.key.isAssignableFrom(type) }.flatMap { it.value }

    private class Subscriber : JedisPubSub() {

        override fun onSubscribe(channel: String, subscribedChannels: Int) {
            isConnected = true
            Logger.info("Redis: listening on '$channel'")
        }

        override fun onUnsubscribe(channel: String, subscribedChannels: Int) {
            isConnected = false
        }

        override fun onMessage(channel: String, message: String) {
            receive(message)
        }
    }
}
