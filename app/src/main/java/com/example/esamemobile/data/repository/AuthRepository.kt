package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gestisce autenticazione (email/password + provider esterni come Google) e la
 * creazione/lettura del profilo utente su Firestore.
 */
class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(email: String, password: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = requireNotNull(result.user) { "Registrazione fallita" }
        saveProfile(UserProfile(user.uid, displayName, email, user.photoUrl?.toString()))
        return user
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user) { "Login fallito" }
    }

    /** Login con credenziale esterna (es. Google ottenuta dal flusso Sign-In). */
    suspend fun loginWithCredential(credential: AuthCredential): FirebaseUser {
        val result = auth.signInWithCredential(credential).await()
        val user = requireNotNull(result.user) { "Login fallito" }
        // Crea il profilo al primo accesso
        val snapshot = db.collection(USERS).document(user.uid).get().await()
        if (!snapshot.exists()) {
            saveProfile(
                UserProfile(user.uid, user.displayName ?: "", user.email ?: "", user.photoUrl?.toString())
            )
        }
        return user
    }

    fun logout() = auth.signOut()

    private suspend fun saveProfile(profile: UserProfile) {
        db.collection(USERS).document(profile.uid).set(profile).await()
    }

    companion object {
        private const val USERS = "users"
    }
}
