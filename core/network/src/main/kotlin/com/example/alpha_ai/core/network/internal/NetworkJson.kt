package com.example.alpha_ai.core.network.internal

import kotlinx.serialization.json.Json

/** The one JSON configuration for provider traffic. Unknown fields are ignored on purpose. */
internal val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
