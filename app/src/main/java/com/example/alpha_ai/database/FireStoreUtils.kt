package com.example.alpha_ai.database

import com.example.alpha_ai.database.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class FireStoreUtils {
    private val userCollectionName = "users"
    private val roomCollectionName = "rooms"
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
}