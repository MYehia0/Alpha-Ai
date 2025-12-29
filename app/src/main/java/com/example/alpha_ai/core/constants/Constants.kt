package com.example.alpha_ai.core.constants

object Constants {

    // Base URL for all Router API endpoints
    const val ROUTER_BASE_URL = "https://router.huggingface.co/"

    // Text Generation LLM (Qwen)
    object TextLLM {
        const val MODEL_ID = "Qwen/Qwen2.5-7B-Instruct"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Language Detection
    object LDF {
        const val MODEL_ID = "papluca/xlm-roberta-base-language-detection"
        const val ENDPOINT = "hf-inference/models/$MODEL_ID"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Machine Translation (English to Arabic)
    object MT {
        const val MODEL_ID = "Helsinki-NLP/opus-mt-en-ar"
        const val ENDPOINT = "hf-inference/models/$MODEL_ID"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Summarization (English)
    object SUM {
        const val MODEL_ID = "google/pegasus-large"
        const val ENDPOINT = "hf-inference/models/$MODEL_ID"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Summarization (Arabic)
    object SUMar {
        const val MODEL_ID = "csebuetnlp/mT5_m2o_arabic_crossSum"
        const val ENDPOINT = "hf-inference/models/$MODEL_ID"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    /*
    // Optical Character Recognition
    object OCR {
        const val API_URL = "https://api-inference.huggingface.co/models/microsoft/trocr-large-handwritten"
        private const val API_TOKEN = "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"
        val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
    }

    // Speech-to-Text
    object SST {
        const val API_URL = "https://api-inference.huggingface.co/models/jonatasgrosman/wav2vec2-large-xlsr-53-english"
        private const val API_TOKEN = "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"
        val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
    }

    // Text-to-Speech
    object TTS {
        const val API_URL = "https://api-inference.huggingface.co/models/facebook/fastspeech2-en-ljspeech"
        private const val API_TOKEN = "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"
        val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
    }

    // Image Generation
    object IG {
        const val API_URL = "https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5"
        private const val API_TOKEN = "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"
        val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    */
}