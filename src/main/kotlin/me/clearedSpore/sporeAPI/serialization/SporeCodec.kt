package me.clearedSpore.sporeAPI.serialization

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface SporeCodec<T> {

    fun encode(value: T): String

    fun decode(data: String): T?
}