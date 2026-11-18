package com.example.alpha_ai.core.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads and writes [SessionData] as encrypted JSON. Anything unreadable (a key lost after a
 * restore, a corrupt file) becomes [SessionData.EMPTY]: the user stays signed in through the
 * sign-in service and the cache is refilled (research.md §3).
 */
internal class SessionSerializer(private val cipher: SessionCipher) : Serializer<SessionData> {

    override val defaultValue: SessionData = SessionData.EMPTY

    override suspend fun readFrom(input: InputStream): SessionData {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            json.decodeFromString(SessionData.serializer(), cipher.decrypt(bytes).decodeToString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only the exception type: its message or the data could describe the user (FR-024).
            Timber.w("Session store unreadable (%s); starting empty", e.javaClass.simpleName)
            defaultValue
        }
    }

    override suspend fun writeTo(t: SessionData, output: OutputStream) {
        val plain = json.encodeToString(SessionData.serializer(), t).encodeToByteArray()
        output.write(cipher.encrypt(plain))
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
