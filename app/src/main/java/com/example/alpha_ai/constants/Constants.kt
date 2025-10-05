package com.example.alpha_ai.constants

object Constants {
    class GEC {
        companion object{
//            const val API_URL = "https://api-inference.huggingface.co/models/gotutiyan/gec-t5-large-clang8"
            const val API_URL = "https://api-inference.huggingface.co/models/grammarly/coedit-large"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class GECar {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/CAMeL-Lab/arabart-qalb15-gec-ged-13"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class LDF {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/facebook/fasttext-language-identification"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class OCR {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/microsoft/trocr-large-handwritten" // https://api-inference.huggingface.co/models/microsoft/trocr-large-printed
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class SST {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/jonatasgrosman/wav2vec2-large-xlsr-53-english" // https://api-inference.huggingface.co/models/openai/whisper-large-v3
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class TTS {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/facebook/fastspeech2-en-ljspeech"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class MT {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/Helsinki-NLP/opus-mt-en-ar"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class TG {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/openai-community/gpt2"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class IG {
        companion object{
            const val API_URL = "https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class SUM {
        companion object{
//            const val API_URL = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn"
            const val API_URL = "https://api-inference.huggingface.co/models/google/pegasus-large"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
    class SUMar {
        companion object{
            //            const val API_URL = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn"
            const val API_URL = "https://api-inference.huggingface.co/models/csebuetnlp/mT5_m2o_arabic_crossSum"
            private const val API_TOKEN = "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"  // Your Hugging Face API token
            val headers = mapOf("Authorization" to "Bearer $API_TOKEN")
        }
    }
}