
package com.example.instagramclone

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        user: User,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
