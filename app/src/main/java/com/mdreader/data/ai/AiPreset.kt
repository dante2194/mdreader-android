package com.mdreader.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiPreset(
    val name: String,
    val prompt: String   // Template with {copied text} placeholder
)
