package com.example.alpha_ai.core.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreSessionCipherTest {

    private val cipher = KeystoreSessionCipher()
    private val plain = "{\"email\":\"ali@example.com\"}".encodeToByteArray()

    @Test
    fun roundTrip() {
        assertArrayEquals(plain, cipher.decrypt(cipher.encrypt(plain)))
    }

    @Test
    fun eachEncryptionUsesAFreshIv() {
        assertFalse(cipher.encrypt(plain).contentEquals(cipher.encrypt(plain)))
    }

    @Test(expected = Exception::class)
    fun tamperedDataIsRejected() {
        val encrypted = cipher.encrypt(plain)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        cipher.decrypt(encrypted)
    }
}
