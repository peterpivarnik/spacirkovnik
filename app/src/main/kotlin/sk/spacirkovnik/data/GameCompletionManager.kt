package sk.spacirkovnik.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class GameCompletionManager {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL)

    fun recordCompletion(gameId: String) {
        val uid = auth.currentUser?.uid ?: return
        database.reference
            .child("completions")
            .child(uid)
            .child(gameId)
            .push()
            .setValue(System.currentTimeMillis())
    }
}
