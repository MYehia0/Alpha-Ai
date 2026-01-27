package com.example.alpha_ai.data.repository

import com.example.alpha_ai.data.local.PreferencesDataSource
import com.example.alpha_ai.data.models.User
import com.example.alpha_ai.data.remote.firebase.FirebaseAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val preferencesDataSource: PreferencesDataSource
) {

    // ========== Authentication Operations ==========
    suspend fun signIn(email: String, password: String, rememberMe: Boolean): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val firebaseUser = firebaseAuthService.signInWithEmailPassword(email, password)
                val user = firebaseAuthService.getUserFromFirestore(firebaseUser.uid)
                preferencesDataSource.saveUserSession(
                    userId = user.uid!!,
                    userName = user.uName,
                    userEmail = user.uEmail
                )
                if (rememberMe) {
                    preferencesDataSource.saveCredentials(email, password)
                } else {
                    preferencesDataSource.clearCredentials()
                }

                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun register(email: String, password: String, fullName: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val firebaseUser = firebaseAuthService.createUserWithEmailPassword(email, password)
                val user = User(
                    uid = firebaseUser.uid,
                    uEmail = email,
                    uName = fullName
                )
                firebaseAuthService.saveUserToFirestore(user)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun attemptAutoLogin(): Result<User>? {
        if (!preferencesDataSource.isRememberMeEnabled()) {
            return null
        }

        val savedEmail = preferencesDataSource.getSavedEmail() ?: return null
        val savedPassword = preferencesDataSource.getSavedPassword() ?: return null

        return signIn(savedEmail, savedPassword, rememberMe = true)
    }

    suspend fun getCurrentUserData(forceRefresh: Boolean = false): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val firebaseUser = firebaseAuthService.getCurrentUser()
                    ?: return@withContext Result.failure(Exception("No authenticated user"))

                if (!forceRefresh) {
                    val cachedUserId = preferencesDataSource.getUserId()
                    val cachedUserName = preferencesDataSource.getUserName()
                    val cachedUserEmail = preferencesDataSource.getUserEmail()

                    if (cachedUserId == firebaseUser.uid && cachedUserName != null) {
                        return@withContext Result.success(
                            User(
                                uid = cachedUserId,
                                uName = cachedUserName,
                                uEmail = cachedUserEmail
                            )
                        )
                    }
                }
                val user = firebaseAuthService.getUserFromFirestore(firebaseUser.uid)
                preferencesDataSource.saveUserSession(
                    userId = user.uid!!,
                    userName = user.uName,
                    userEmail = user.uEmail
                )
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== User Profile Operations ==========

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuthService.updateUserInFirestore(userId, updates)
                updates["uName"]?.let { name ->
                    val currentId = preferencesDataSource.getUserId()
                    val currentEmail = preferencesDataSource.getUserEmail()
                    if (currentId == userId) {
                        preferencesDataSource.saveUserSession(userId, name.toString(), currentEmail)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== Password Operations ==========

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuthService.sendPasswordResetEmail(email)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateEmail(newEmail: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val userId = firebaseAuthService.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("No authenticated user"))

                firebaseAuthService.updateEmail(newEmail)
                firebaseAuthService.updateUserInFirestore(userId, mapOf("uEmail" to newEmail))

                val currentName = preferencesDataSource.getUserName()
                preferencesDataSource.saveUserSession(userId, currentName, newEmail)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updatePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuthService.updatePassword(newPassword)

                if (preferencesDataSource.isRememberMeEnabled()) {
                    val email = preferencesDataSource.getSavedEmail()
                    if (email != null) {
                        preferencesDataSource.saveCredentials(email, newPassword)
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== Account Operations ==========

    suspend fun deleteAccount(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuthService.deleteAccount()
                clearLocalData()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun signOut() {
        firebaseAuthService.signOut()
        clearLocalData()
    }

    private fun clearLocalData() {
        preferencesDataSource.clearAll()
    }

    // ========== Session Queries ==========

    fun isUserLoggedIn(): Boolean {
        return firebaseAuthService.isUserLoggedIn()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuthService.getCurrentUserId()
    }

    fun isRememberMeEnabled(): Boolean {
        return preferencesDataSource.isRememberMeEnabled()
    }

    fun getSavedCredentials(): Pair<String, String>? {
        val email = preferencesDataSource.getSavedEmail()
        val password = preferencesDataSource.getSavedPassword()
        return if (email != null && password != null) {
            Pair(email, password)
        } else null
    }
}