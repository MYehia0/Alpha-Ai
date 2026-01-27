package com.example.alpha_ai.data.remote.firebase

import com.example.alpha_ai.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    suspend fun signInWithEmailPassword(email: String, password: String): FirebaseUser {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        return authResult.user ?: throw Exception("Authentication succeeded but user is null")
    }

    suspend fun createUserWithEmailPassword(email: String, password: String): FirebaseUser {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        return authResult.user ?: throw Exception("Registration succeeded but user is null")
    }

    suspend fun getUserFromFirestore(userId: String): User {
        val document = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        return document.toObject(User::class.java)
            ?: throw Exception("User data not found in database")
    }

    suspend fun saveUserToFirestore(user: User) {
        if (user.uid == null) {
            throw IllegalArgumentException("User ID cannot be null")
        }

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun updateUserInFirestore(userId: String, updates: Map<String, Any>) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update(updates)
            .await()
    }

    suspend fun deleteUserFromFirestore(userId: String) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .delete()
            .await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateEmail(newEmail: String) {
        auth.currentUser?.updateEmail(newEmail)?.await()
            ?: throw Exception("No authenticated user")
    }

    suspend fun updatePassword(newPassword: String) {
        auth.currentUser?.updatePassword(newPassword)?.await()
            ?: throw Exception("No authenticated user")
    }

    suspend fun deleteAccount() {
        val userId = auth.currentUser?.uid
            ?: throw Exception("No authenticated user")

        deleteUserFromFirestore(userId)

        auth.currentUser?.delete()?.await()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = auth.signOut()

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}