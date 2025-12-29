package com.example.alpha_ai.data.firebase

import com.example.alpha_ai.data.models.User
import com.example.alpha_ai.data.models.History
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FireStoreUtils {
    private val userCollectionName = "users"
    private fun createCollections(collectionName:String?):CollectionReference{
        val db = FirebaseFirestore.getInstance()
        return db.collection(collectionName!!)
    }
    fun insertUserToFireStore(user: User?): Task<Void> {
        val dbRef = createCollections(userCollectionName).document(user?.uid!!)
        return dbRef.set(user)
    }
    fun getUserFromFireStore(userID:String?):Task<DocumentSnapshot>{
        val dbRef = createCollections(userCollectionName).document(userID!!)
        return dbRef.get()
    }
    fun sendHistory(history: History):Task<Void> {
        val dbRef = createCollections(userCollectionName).document(history.uid!!)
        val historyRef = dbRef.collection("histories").document()
        history.id = historyRef.id
        return historyRef.set(history)
    }

    fun getHistory(userID:String?): Task<QuerySnapshot> {
        val dbRef = createCollections(userCollectionName).document(userID!!)
        val historyRef = dbRef.collection("histories")
        return historyRef.get()
    }

}