package me.clearedSpore.sporeAPI.scan

import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.plugin.java.JavaPlugin
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.vfs.Vfs
import java.lang.reflect.Modifier
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeScanner {

    private val cache = ConcurrentHashMap<String, Reflections>()

    @Volatile
    private var vfsConfigured = false

    fun forPlugin(plugin: JavaPlugin): Reflections =
        cache.computeIfAbsent(plugin.name) { build(plugin) }

    fun annotatedWith(plugin: JavaPlugin, annotation: Class<out Annotation>): Set<Class<*>> =
        forPlugin(plugin).getTypesAnnotatedWith(annotation)

    fun invalidate(plugin: JavaPlugin) {
        cache.remove(plugin.name)
    }

    fun instantiate(clazz: Class<*>): Any =
        objectInstanceOrNull(clazz)
            ?: clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

    fun objectInstanceOrNull(clazz: Class<*>): Any? {
        return try {
            val field = clazz.getDeclaredField("INSTANCE")
            if (Modifier.isStatic(field.modifiers) && clazz.isAssignableFrom(field.type)) {
                field.isAccessible = true
                field.get(null)
            } else {
                null
            }
        } catch (exception: NoSuchFieldException) {
            null
        }
    }

    private fun build(plugin: JavaPlugin): Reflections {
        configureVfs()

        val builder = ConfigurationBuilder()
            .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
            .addClassLoaders(plugin.javaClass.classLoader)

        val jarUrl = jarUrlOf(plugin)
        if (jarUrl != null) {
            builder.setUrls(jarUrl)
        } else {
            Logger.warn("Could not locate the jar for ${plugin.name}, falling back to package scanning")
            builder.forPackage(plugin.javaClass.`package`.name, plugin.javaClass.classLoader)
        }

        return Reflections(builder)
    }

    private fun jarUrlOf(plugin: JavaPlugin): URL? =
        runCatching { plugin.javaClass.protectionDomain?.codeSource?.location }.getOrNull()

    private fun configureVfs() {
        if (vfsConfigured) return
        vfsConfigured = true

        Vfs.addDefaultURLTypes(object : Vfs.UrlType {
            override fun matches(url: URL): Boolean =
                url.protocol == "file" && url.toExternalForm().endsWith(".jar")

            override fun createDir(url: URL): Vfs.Dir =
                Vfs.DefaultUrlTypes.jarFile.createDir(url)
        })
    }
}
