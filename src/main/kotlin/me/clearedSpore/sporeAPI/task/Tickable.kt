package me.clearedSpore.sporeAPI.task

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface Tickable {
    fun tick()
    fun isFinished(): Boolean
}