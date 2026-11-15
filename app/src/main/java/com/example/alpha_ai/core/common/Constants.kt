package com.example.alpha_ai.core.common

import com.example.alpha_ai.BuildConfig

object Constants {

    // Base URL for all HuggingFace Router APIs
    const val ROUTER_BASE_URL = "https://router.huggingface.co/"

    // The token comes from local.properties (HF_API_TOKEN) and is never committed.
    private val HF_AUTH_HEADER = "Bearer ${BuildConfig.HF_API_TOKEN}"

    // Text Generation LLM (Qwen) - Uses Chat Completions endpoint
    object TextLLM {
        const val MODEL_ID = "Qwen/Qwen2.5-7B-Instruct"
        val AUTH_HEADER = HF_AUTH_HEADER
    }

    // Language Detection - Uses Inference API
    object LDF {
        const val ENDPOINT = "hf-inference/models/papluca/xlm-roberta-base-language-detection"
        val AUTH_HEADER = HF_AUTH_HEADER
    }

    // Machine Translation (English to Arabic) - Uses Inference API
    object MT {
        const val ENDPOINT = "hf-inference/models/Helsinki-NLP/opus-mt-en-ar"
        val AUTH_HEADER = HF_AUTH_HEADER
    }

    // Summarization (English) - Uses Inference API
    object SUM {
        const val ENDPOINT = "hf-inference/models/google/pegasus-large"
        val AUTH_HEADER = HF_AUTH_HEADER
    }

    // Summarization (Arabic) - Uses Inference API
    object SUMar {
        const val ENDPOINT = "hf-inference/models/csebuetnlp/mT5_m2o_arabic_crossSum"
        val AUTH_HEADER = HF_AUTH_HEADER
    }
}
