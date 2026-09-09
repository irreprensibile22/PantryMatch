package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// Autenticazione (email/password + Google) e gestione account, appoggiata a FirebaseAuth + Firestore.
class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(email: String, password: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user) { "Registrazione fallita" }
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()
        saveProfile(UserProfile(user.uid, displayName, email.trim().lowercase(), user.photoUrl?.toString()))
        return user
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user) { "Login fallito" }
    }

    suspend fun loginWithCredential(credential: AuthCredential): FirebaseUser {
        val result = auth.signInWithCredential(credential).await()
        val user = requireNotNull(result.user) { "Login fallito" }
        // primo accesso con Google: creo il profilo Firestore (funge da registrazione)
        val snapshot = db.collection(USERS).document(user.uid).get().await()
        if (!snapshot.exists()) {
            saveProfile(
                UserProfile(
                    user.uid,
                    user.displayName ?: "",
                    user.email?.lowercase() ?: "",
                    user.photoUrl?.toString(),
                )
            )
        }
        return user
    }

    fun logout() = auth.signOut()

    suspend fun updateDisplayName(name: String) {
        val user = requireNotNull(auth.currentUser) { "Utente non autenticato" }
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        ).await()
        db.collection(USERS).document(user.uid)
            .set(mapOf("displayName" to name.trim()), SetOptions.merge()).await()
    }

    // Firebase richiede un accesso recente per cambiare email/password: ri-autentico prima
    private suspend fun reauthenticate(currentPassword: String) {
        val user = requireNotNull(auth.currentUser) { "Utente non autenticato" }
        val email = requireNotNull(user.email) { "Email non disponibile" }
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        reauthenticate(currentPassword)
        requireNotNull(auth.currentUser) { "Utente non autenticato" }
            .updatePassword(newPassword).await()
    }

    suspend fun changeEmail(currentPassword: String, newEmail: String) {
        reauthenticate(currentPassword)
        // il cambio diventa effettivo solo dopo che l'utente conferma dal link ricevuto via mail
        requireNotNull(auth.currentUser) { "Utente non autenticato" }
            .verifyBeforeUpdateEmail(newEmail.trim()).await()
    }

    sealed interface EmailSync {
        
        data object Unchanged : EmailSync
        
        data class Updated(val email: String) : EmailSync
        
        data object SessionRevoked : EmailSync
    }

    // verifyBeforeUpdateEmail non avvisa a cambio avvenuto: riallineo qui il doc Firestore all'email verificata
    suspend fun reconcileEmail(): EmailSync {
        val user = auth.currentUser ?: return EmailSync.Unchanged
        try {
            user.reload().await()
            // il cambio email primaria revoca la sessione: forzo un refresh del token per accorgermene
            user.getIdToken(true).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            auth.signOut()
            return EmailSync.SessionRevoked
        }
        val authEmail = auth.currentUser?.email?.lowercase() ?: return EmailSync.Unchanged
        val doc = db.collection(USERS).document(user.uid)
        val storedEmail = doc.get().await().getString("email")
        if (storedEmail == authEmail) return EmailSync.Unchanged
        doc.set(mapOf("email" to authEmail), SetOptions.merge()).await()
        return EmailSync.Updated(authEmail)
    }

    suspend fun sendPasswordReset(): String {
        val email = requireNotNull(auth.currentUser?.email) { "Email non disponibile" }
        auth.sendPasswordResetEmail(email).await()
        return email
    }

    private suspend fun saveProfile(profile: UserProfile) {
        db.collection(USERS).document(profile.uid).set(profile).await()
    }

    companion object {
        private const val USERS = "users"
    }
}
