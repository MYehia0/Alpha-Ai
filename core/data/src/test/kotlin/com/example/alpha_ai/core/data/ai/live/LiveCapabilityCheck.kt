package com.example.alpha_ai.core.data.ai.live

import com.example.alpha_ai.core.data.ai.DefaultGrammarCorrector
import com.example.alpha_ai.core.data.ai.DefaultImageTextExtractor
import com.example.alpha_ai.core.data.ai.DefaultSpeechTranscriber
import com.example.alpha_ai.core.data.ai.DefaultSummarizer
import com.example.alpha_ai.core.data.ai.DefaultTranslator
import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.config.HuggingFaceModels
import com.example.alpha_ai.core.data.ai.config.ModelChains
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.data.ai.config.ModelConfigResolver
import com.example.alpha_ai.core.data.ai.config.NoOverrides
import com.example.alpha_ai.core.data.ai.config.OpenRouterModels
import com.example.alpha_ai.core.data.ai.engine.ModelInvoker
import com.example.alpha_ai.core.data.ai.engine.TextTaskRunner
import com.example.alpha_ai.core.data.ai.fallback.AiClock
import com.example.alpha_ai.core.data.ai.fallback.AiDiagnostics
import com.example.alpha_ai.core.data.ai.fallback.FallbackRunner
import com.example.alpha_ai.core.data.ai.fallback.ModelHealthTracker
import com.example.alpha_ai.core.data.ai.fallback.StreamingFallbackRunner
import com.example.alpha_ai.core.data.ai.validate.ScriptAnalyzer
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.network.AiClientFactory
import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ConnectivityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.File

/**
 * Calls the real providers with the samples in `src/test/resources/ai-samples/`. It spends
 * requests from the daily free allowance, so it never runs by default:
 *
 * ```
 * ./gradlew :core:data:testDebugUnitTest -PliveAi -Pcapability=grammar
 * ```
 *
 * `capability` is one of `grammar`, `translation`, `summarization`, `image`, `speech`, and
 * is required, so a run can never spend more than one capability's requests. Add `-Pcompare`
 * to run every candidate model on its own instead of the configured chain. A results table
 * is written to `build/reports/ai-live/`.
 *
 * Without `-PliveAi`, or without `OPENROUTER_API_KEY`, every test here is skipped and
 * nothing is sent. CI never sets the property.
 */
class LiveCapabilityCheck {

    private val key: String? = System.getenv("OPENROUTER_API_KEY")?.takeIf { it.isNotBlank() }
    private val selected: String = System.getProperty("liveAi.capability").orEmpty()
    private val compare: Boolean = System.getProperty("liveAi.compare") == "true"
    private val reportDir = File(System.getProperty("liveAi.reportDir") ?: "build/reports/ai-live")
    private val samplesDir = File("src/test/resources/ai-samples")

    private val rows = mutableListOf<Row>()
    private var answeredBy = "-"

    private class Row(
        val model: String,
        val sample: String,
        val language: String,
        val result: String,
        val millis: Long,
        val rightLanguage: String,
        val matchesExpected: String,
        val output: String,
    )

    @Before
    fun setUp() {
        assumeTrue("live check not requested (-PliveAi)", System.getProperty("liveAi") == "true")
        assumeTrue("OPENROUTER_API_KEY is not set", key != null)
        // Print the layer's diagnostics (they hold no input, output, or key) and record which
        // model answered.
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (tag == AiDiagnostics.LOG_TAG || tag == "AiNetwork" || tag == "AiConfig") println("[$tag] $message")
                if (tag == AiDiagnostics.LOG_TAG && "outcome=SUCCESS" in message) {
                    answeredBy = message.substringAfter("model=").substringBefore(' ')
                }
            }
        })
    }

    @After
    fun tearDown() = Timber.uprootAll()

    // ---- the checks -----------------------------------------------------------------------

    @Test
    fun grammar() = textCheck("grammar", Capability.GRAMMAR) { layer, sample ->
        layer.grammar.correct(sample.input, sample.language)
    }

    @Test
    fun translation() = textCheck("translation", Capability.TRANSLATION) { layer, sample ->
        layer.translator.translate(sample.input, sample.language, sample.language.other())
    }

    @Test
    fun summarization() = textCheck("summarization", Capability.SUMMARIZATION) { layer, sample ->
        layer.summarizer.summarize(sample.input, sample.language)
    }

    @Test
    fun image() = mediaCheck("image", Capability.IMAGE_TEXT, "images", IMAGE_TYPES) { layer, file, mime, _ ->
        layer.imageText.extractText(ImageInput(file.readBytes(), mime))
    }

    @Test
    fun speech() = mediaCheck("speech", Capability.SPEECH, "audio", AUDIO_TYPES) { layer, file, mime, language ->
        // The duration is not read from the file; samples must be shorter than the limit.
        layer.speech.transcribe(AudioClip(file.readBytes(), mime, durationMillis = 1), language)
    }

    // ---- running --------------------------------------------------------------------------

    private fun textCheck(
        name: String,
        capability: Capability,
        call: suspend (Layer, TextSample) -> AiResult<String>,
    ) {
        assumeTrue("capability '$name' not selected (-Pcapability=$name)", selected == name)
        val samples = TextSample.readAll(File(samplesDir, "$name.txt"))
        for (layer in layers(capability)) {
            for (sample in samples) {
                // The result of a translation is in the other language.
                val expectedLanguage = if (name == "translation") sample.language.other() else sample.language
                record(layer, sample.id, sample.language, sample.expected, expectedLanguage) { call(layer, sample) }
            }
        }
        writeReport(name)
    }

    private fun mediaCheck(
        name: String,
        capability: Capability,
        folder: String,
        types: Map<String, String>,
        call: suspend (Layer, File, String, Language) -> AiResult<String>,
    ) {
        assumeTrue("capability '$name' not selected (-Pcapability=$name)", selected == name)
        val files = File(samplesDir, folder).listFiles { file -> file.extension.lowercase() in types }
            .orEmpty().sortedBy { it.name }
        assumeTrue("no samples in ${File(samplesDir, folder).path}; see its README", files.isNotEmpty())
        for (layer in layers(capability)) {
            for (file in files) {
                // Files are named by language: ar-1.png, en-2.wav. The expected text sits
                // beside each one in a .txt file of the same name.
                val language = if (file.name.startsWith("ar")) Language.ARABIC else Language.ENGLISH
                val expected = File(file.parentFile, "${file.nameWithoutExtension}.txt")
                    .takeIf { it.isFile }?.readText()?.trim()
                val mime = types.getValue(file.extension.lowercase())
                record(layer, file.name, language, expected, language) { call(layer, file, mime, language) }
            }
        }
        writeReport(name)
    }

    private fun record(
        layer: Layer,
        sample: String,
        language: Language,
        expected: String?,
        expectedLanguage: Language,
        call: suspend () -> AiResult<String>,
    ) {
        answeredBy = "-"
        val startedAt = System.currentTimeMillis()
        val result = runBlocking { call() }
        val millis = System.currentTimeMillis() - startedAt
        val output = (result as? AiResult.Success)?.value
        val row = Row(
            model = layer.label ?: answeredBy,
            sample = sample,
            language = language.name,
            result = when (result) {
                is AiResult.Success -> "ok"
                is AiResult.Failure -> result.failure.toString()
            },
            millis = millis,
            rightLanguage = output?.takeIf { it.isNotEmpty() }
                ?.let { if (ScriptAnalyzer.isAcceptedAs(it, expectedLanguage)) "yes" else "NO" } ?: "-",
            matchesExpected = when {
                output == null || expected == null -> "-"
                output.normalized() == expected.normalized() -> "exact"
                else -> "judge"
            },
            output = output.orEmpty(),
        )
        rows += row
        println("[live] ${row.model} | ${row.sample} | ${row.result} | ${row.millis} ms | ${row.output}")
    }

    private fun writeReport(name: String) {
        reportDir.mkdirs()
        val file = File(reportDir, if (compare) "$name-compare.md" else "$name.md")
        val table = buildString {
            appendLine("# Live check: $name" + if (compare) " (comparison)" else "")
            appendLine()
            appendLine("`matches expected`: `exact` is a full match; `judge` needs a person to score it 0 to 2.")
            appendLine()
            appendLine("| Model | Sample | Language | Result | Time (ms) | Right language | Matches expected | Output |")
            appendLine("|---|---|---|---|---|---|---|---|")
            for (row in rows) {
                val output = row.output.replace("|", "\\|").replace("\n", "<br>")
                appendLine(
                    "| `${row.model}` | ${row.sample} | ${row.language} | ${row.result} | ${row.millis} | " +
                        "${row.rightLanguage} | ${row.matchesExpected} | $output |",
                )
            }
        }
        file.writeText(table)
        println("[live] results written to ${file.path}")
    }

    // ---- building the real layer ----------------------------------------------------------

    /** The configured chain, or with `-Pcompare` one single-model layer per candidate. */
    private fun layers(capability: Capability): List<Layer> =
        if (compare) {
            candidates(capability).map { modelId ->
                val only = ModelChoice(AiProvider.OPENROUTER, modelId, rank = 10)
                Layer(label = modelId, chains = { listOf(only) })
            }
        } else {
            listOf(Layer(label = null, chains = ModelConfigResolver(NoOverrides())::chainFor))
        }

    /**
     * The models to compare: every one configured for the capability, plus the candidates
     * from research.md section 3 that are not in a chain. The free router is left out
     * because it is not one model.
     */
    private fun candidates(capability: Capability): List<String> {
        val configured = OpenRouterModels.choices[capability].orEmpty().map { it.modelId }
            .filterNot { it == "openrouter/free" }
        val extra = if (capability == Capability.SPEECH) emptyList() else listOf("inclusionai/ling-3.2-flash:free")
        return (configured + extra).distinct()
    }

    private inner class Layer(val label: String?, chains: suspend (Capability) -> List<ModelChoice>) {
        private val credentials = object : AiCredentials {
            override fun tokenFor(provider: AiProvider): String? = key.takeIf { provider == AiProvider.OPENROUTER }
        }
        private val modelChains = object : ModelChains {
            override suspend fun chainFor(capability: Capability) = chains(capability)
        }
        private val clock = AiClock { System.currentTimeMillis() }
        private val health = ModelHealthTracker(clock)
        private val online = object : ConnectivityChecker {
            override fun isOnline(): Boolean = true
        }
        private val runner = FallbackRunner(modelChains, health, online, clock)
        private val invoker = ModelInvoker(
            AiClientFactory.openRouter(credentials, OpenRouterModels.baseUrl),
            AiClientFactory.huggingFace(credentials, HuggingFaceModels.baseUrl),
        )
        private val tasks = TextTaskRunner(runner, StreamingFallbackRunner(modelChains, health, online, clock), invoker)

        val grammar = DefaultGrammarCorrector(tasks)
        val translator = DefaultTranslator(tasks)
        val summarizer = DefaultSummarizer(tasks)
        val imageText = DefaultImageTextExtractor(runner, invoker, Dispatchers.Default)
        val speech = DefaultSpeechTranscriber(runner, invoker, Dispatchers.Default)
    }

    private fun Language.other(): Language = if (this == Language.ARABIC) Language.ENGLISH else Language.ARABIC

    private fun String.normalized(): String = trim().replace(Regex("\\s+"), " ")

    private companion object {
        val IMAGE_TYPES = mapOf("png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "webp" to "image/webp")
        val AUDIO_TYPES = mapOf("wav" to "audio/wav", "mp3" to "audio/mpeg", "m4a" to "audio/mp4")
    }
}

/**
 * One text sample. A samples file holds blocks separated by a line of `---`; each block has
 * the lines `id:`, `lang:` (`ar` or `en`), `input:`, and optionally `expected:`.
 */
internal class TextSample(val id: String, val language: Language, val input: String, val expected: String?) {
    companion object {
        fun readAll(file: File): List<TextSample> =
            file.readText().split(Regex("(?m)^---\\s*$")).mapNotNull { block ->
                val fields = block.lines().filter { ':' in it && !it.startsWith("#") }
                    .associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }
                val input = fields["input"] ?: return@mapNotNull null
                TextSample(
                    id = fields["id"] ?: "?",
                    language = if (fields["lang"] == "ar") Language.ARABIC else Language.ENGLISH,
                    input = input,
                    expected = fields["expected"],
                )
            }
    }
}
