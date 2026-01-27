package com.example.alpha_ai.data.repository

import com.example.alpha_ai.data.models.History
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun saveHistory(history: History): Result<History> {
        return try {
            val userDoc = firestore.collection("users").document(history.uid!!)
            val historyRef = userDoc.collection("histories").document()

            val historyWithId = history.copy(id = historyRef.id)
            historyRef.set(historyWithId).await()
            Result.success(historyWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistories(userId: String): Result<List<History>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("histories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val histories = snapshot.toObjects(History::class.java)
            Result.success(histories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHistory(userId: String, historyId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("histories")
                .document(historyId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistoriesByType(userId: String, type: String): Result<List<History>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("histories")
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val histories = snapshot.toObjects(History::class.java)
            Result.success(histories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}