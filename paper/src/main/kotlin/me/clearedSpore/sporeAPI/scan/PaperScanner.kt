package me.clearedSpore.sporeAPI.scan

import me.clearedSpore.sporeAPI.registry.Registry
import org.bukkit.plugin.java.JavaPlugin
import org.reflections.Reflections

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


fun SporeScanner.forPlugin(plugin: JavaPlugin): Reflections = forClass(plugin.javaClass)

fun SporeScanner.annotatedWith(plugin: JavaPlugin, annotation: Class<out Annotation>): Set<Class<*>> =
    annotatedWith(plugin.javaClass, annotation)

fun SporeScanner.methodsAnnotatedWith(plugin: JavaPlugin, annotation: Class<out Annotation>): Set<java.lang.reflect.Method> =
    methodsAnnotatedWith(plugin.javaClass, annotation)

fun SporeScanner.invalidate(plugin: JavaPlugin) = invalidate(plugin.javaClass)

fun Registry<*>.scan(plugin: JavaPlugin): Int = scan(plugin.javaClass)
