package com.example.alpha_ai.core.network

/**
 * A model's answer.
 *
 * @param servedBy the model that answered, when the provider reports it
 * @param truncated true when the answer was cut off by the token limit
 */
data class ChatReply(
    val text: String,
    val servedBy: String?,
    val truncated: Boolean,
)
