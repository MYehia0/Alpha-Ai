package com.example.alpha_ai.core.datastore

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SessionSerializerTest {

    /** Reversible stand-in for the Keystore cipher. */
    private class XorCipher : SessionCipher {
        override fun encrypt(plain: ByteArray) = ByteArray(plain.size) { (plain[it].toInt() xor 0x5A).toByte() }

        override fun decrypt(encrypted: ByteArray) = encrypt(encrypted)
    }

    private class BrokenCipher : SessionCipher {
        override fun encrypt(plain: ByteArray) = plain

        override fun decrypt(encrypted: ByteArray): ByteArray = throw IllegalStateException("key gone")
    }

    private val full = SessionData(
        userId = "uid-1",
        name = "علي",
        email = "ali@example.com",
        emailConfirmed = true,
        profileSyncPending = true,
        confirmationNoticePending = true,
    )

    private suspend fun write(serializer: SessionSerializer, data: SessionData): ByteArray =
        ByteArrayOutputStream().also { serializer.writeTo(data, it) }.toByteArray()

    @Test
    fun `round trip keeps every field`() = runTest {
        val serializer = SessionSerializer(XorCipher())
        val bytes = write(serializer, full)
        assertEquals(full, serializer.readFrom(ByteArrayInputStream(bytes)))
    }

    @Test
    fun `stored bytes do not contain the email in plain text`() = runTest {
        val bytes = write(SessionSerializer(XorCipher()), full)
        assertFalse(bytes.decodeToString().contains("ali@example.com"))
    }

    @Test
    fun `empty file reads as empty`() = runTest {
        assertEquals(SessionData.EMPTY, SessionSerializer(XorCipher()).readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun `garbage reads as empty`() = runTest {
        val garbage = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        assertEquals(SessionData.EMPTY, SessionSerializer(XorCipher()).readFrom(ByteArrayInputStream(garbage)))
    }

    @Test
    fun `a cipher that cannot decrypt reads as empty`() = runTest {
        val bytes = write(SessionSerializer(XorCipher()), full)
        assertEquals(SessionData.EMPTY, SessionSerializer(BrokenCipher()).readFrom(ByteArrayInputStream(bytes)))
    }
}
