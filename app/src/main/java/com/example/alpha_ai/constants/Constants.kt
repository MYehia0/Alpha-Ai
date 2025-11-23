package com.example.alpha_ai.constants
import com.example.alpha_ai.BuildConfig

object Constant {
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


object Constants {
    object GEC {
        const val MODEL_PATH = "models/grammarly/coedit-large"
        val API_TOKEN: String get() = BuildConfig.GEC_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object GECar {
        const val MODEL_PATH = "models/CAMeL-Lab/arabart-qalb15-gec-ged-13"
        val API_TOKEN: String get() = BuildConfig.GECAR_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object LDF {
        const val MODEL_PATH = "models/facebook/fasttext-language-identification"
        val API_TOKEN: String get() = BuildConfig.LDF_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object OCR {
        const val MODEL_PATH = "models/microsoft/trocr-large-handwritten"
        val API_TOKEN: String get() = BuildConfig.OCR_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object SST {
        const val MODEL_PATH = "models/jonatasgrosman/wav2vec2-large-xlsr-53-english"
        val API_TOKEN: String get() = BuildConfig.SST_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object TTS {
        const val MODEL_PATH = "models/facebook/fastspeech2-en-ljspeech"
        val API_TOKEN: String get() = BuildConfig.TTS_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object MT {
        const val MODEL_PATH = "models/Helsinki-NLP/opus-mt-en-ar"
        val API_TOKEN: String get() = BuildConfig.MT_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object TG {
        const val MODEL_PATH = "models/openai-community/gpt2"
        val API_TOKEN: String get() = BuildConfig.TG_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object IG {
        const val MODEL_PATH = "models/runwayml/stable-diffusion-v1-5"
        val API_TOKEN: String get() = BuildConfig.IG_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object SUM {
        const val MODEL_PATH = "models/google/pegasus-large"
        val API_TOKEN: String get() = BuildConfig.SUM_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
    object SUMar {
        const val MODEL_PATH = "models/csebuetnlp/mT5_m2o_arabic_crossSum"
        val API_TOKEN: String get() = BuildConfig.SUMMAR_API_KEY
        val headers: Map<String, String> get() = mapOf("Authorization" to "Bearer $API_TOKEN")
    }
}