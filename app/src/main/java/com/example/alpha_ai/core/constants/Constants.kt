package com.example.alpha_ai.core.constants

object Constants {

    // Base URL for all HuggingFace Router APIs
    const val ROUTER_BASE_URL = "https://router.huggingface.co/"

    // Text Generation LLM (Qwen) - Uses Chat Completions endpoint
    object TextLLM {
        const val MODEL_ID = "Qwen/Qwen2.5-7B-Instruct"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Language Detection - Uses Inference API
    object LDF {
        const val ENDPOINT = "hf-inference/models/papluca/xlm-roberta-base-language-detection"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Machine Translation (English to Arabic) - Uses Inference API
    object MT {
        const val ENDPOINT = "hf-inference/models/Helsinki-NLP/opus-mt-en-ar"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Summarization (English) - Uses Inference API
    object SUM {
        const val ENDPOINT = "hf-inference/models/google/pegasus-large"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Summarization (Arabic) - Uses Inference API
    object SUMar {
        const val ENDPOINT = "hf-inference/models/csebuetnlp/mT5_m2o_arabic_crossSum"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Uncomment and configure these as needed:

    /*
    // Optical Character Recognition - Uses Inference API
    object OCR {
        const val ENDPOINT = "hf-inference/models/microsoft/trocr-large-handwritten"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Speech-to-Text - Uses Inference API
    object SST {
        const val ENDPOINT = "hf-inference/models/jonatasgrosman/wav2vec2-large-xlsr-53-english"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Text-to-Speech - Uses Inference API
    object TTS {
        const val ENDPOINT = "hf-inference/models/facebook/fastspeech2-en-ljspeech"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }

    // Image Generation - Uses Inference API
    object IG {
        const val ENDPOINT = "hf-inference/models/runwayml/stable-diffusion-v1-5"
        private const val API_TOKEN = "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"
        const val AUTH_HEADER = "Bearer $API_TOKEN"
    }
    */
}