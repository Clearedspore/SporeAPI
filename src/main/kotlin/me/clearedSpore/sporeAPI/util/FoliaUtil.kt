package me.clearedSpore.sporeAPI.util

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object FoliaUtil {

    val isFolia: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
