package me.clearedSpore.sporeAPI.scan

import me.clearedSpore.sporeAPI.util.Logger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.vfs.Vfs
import java.lang.reflect.Modifier
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeScanner {

    private val cache = ConcurrentHashMap<String, Reflections>()

    @Volatile
    private var vfsConfigured = false

    // The main class decides what gets scanned: its jar, or its package when the jar can't be found
    fun forClass(mainClass: Class<*>): Reflections =
        cache.computeIfAbsent(mainClass.name) { build(mainClass) }

    fun annotatedWith(mainClass: Class<*>, annotation: Class<out Annotation>): Set<Class<*>> =
        forClass(mainClass).getTypesAnnotatedWith(annotation)

    fun methodsAnnotatedWith(mainClass: Class<*>, annotation: Class<out Annotation>): Set<java.lang.reflect.Method> =
        forClass(mainClass).getMethodsAnnotatedWith(annotation)

    fun invalidate(mainClass: Class<*>) {
        cache.remove(mainClass.name)
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

    private fun build(mainClass: Class<*>): Reflections {
        configureVfs()

        val builder = ConfigurationBuilder()
            .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.MethodsAnnotated)
            .addClassLoaders(mainClass.classLoader)

        val jarUrl = jarUrlOf(mainClass)
        if (jarUrl != null) {
            builder.setUrls(jarUrl)
        } else {
            Logger.warn("Could not locate the jar for ${mainClass.simpleName}, falling back to package scanning")
            builder.forPackage(mainClass.`package`.name, mainClass.classLoader)
        }

        return Reflections(builder)
    }

    private fun jarUrlOf(mainClass: Class<*>): URL? =
        runCatching { mainClass.protectionDomain?.codeSource?.location }.getOrNull()

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
