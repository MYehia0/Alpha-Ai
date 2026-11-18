package com.example.alpha_ai.core.datastore

/** Encrypts the session file. [decrypt] throws when the data cannot be decrypted. */
interface SessionCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(encrypted: ByteArray): ByteArray
}
