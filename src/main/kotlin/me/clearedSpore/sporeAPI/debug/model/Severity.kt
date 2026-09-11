package me.clearedSpore.sporeAPI.debug.model

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


enum class Severity {
    // Something failed, but there was a fallback.
    WARNING,

    // A feature broke for someone.
    ERROR,

    // The plugin can't do its job.
    CRITICAL
}
