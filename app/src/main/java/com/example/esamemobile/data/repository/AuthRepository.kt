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

/**
 * Gestisce autenticazione (email/password + provider esterni come Google) e la
 * creazione/lettura del profilo utente su Firestore.
 */
class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(email: String, password: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user) { "Registrazione fallita" }
        // Aggiorna il displayName anche su FirebaseAuth (oltre al profilo Firestore)
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()
        // Email salvata normalizzata (lowercase) per la ricerca amici per email.
        saveProfile(UserProfile(user.uid, displayName, email.trim().lowercase(), user.photoUrl?.toString()))
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

    /** Aggiorna il nome visualizzato su FirebaseAuth e sul profilo Firestore. */
    suspend fun updateDisplayName(name: String) {
        val user = requireNotNull(auth.currentUser) { "Utente non autenticato" }
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        ).await()
        db.collection(USERS).document(user.uid)
            .set(mapOf("displayName" to name.trim()), SetOptions.merge()).await()
    }

    /**
     * Re-autentica l'utente con la password attuale: operazioni sensibili come il
     * cambio di email/password richiedono un accesso recente.
     */
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

    /**
     * Avvia il cambio email: invia una verifica al nuovo indirizzo; l'email
     * effettiva cambia solo dopo che l'utente conferma dal link.
     */
    suspend fun changeEmail(currentPassword: String, newEmail: String) {
        reauthenticate(currentPassword)
        requireNotNull(auth.currentUser) { "Utente non autenticato" }
            .verifyBeforeUpdateEmail(newEmail.trim()).await()
    }

    /** Esito del riallineamento email (vedi [reconcileEmail]). */
    sealed interface EmailSync {
        /** Nessun cambiamento: email già allineata. */
        data object Unchanged : EmailSync
        /** Email del profilo riallineata al nuovo valore verificato. */
        data class Updated(val email: String) : EmailSync
        /** Sessione revocata (tipico dopo un cambio email confermato): serve ri-login. */
        data object SessionRevoked : EmailSync
    }

    /**
     * Riallinea l'email del profilo Firestore con quella (verificata) di
     * FirebaseAuth. `verifyBeforeUpdateEmail` non notifica l'avvenuto cambio:
     * qui ricarichiamo l'utente e, se l'email è cambiata rispetto al documento,
     * lo aggiorniamo (lowercase, come la registrazione, per la ricerca amici).
     *
     * Cambiando l'email primaria, Firebase **revoca la sessione**: in tal caso il
     * refresh del token fallisce, facciamo `signOut()` e segnaliamo
     * [EmailSync.SessionRevoked] così la UI può reindirizzare al login.
     */
    suspend fun reconcileEmail(): EmailSync {
        val user = auth.currentUser ?: return EmailSync.Unchanged
        try {
            user.reload().await()
            // Forza il refresh del token: se la sessione è revocata lancia qui.
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

    /** Invia l'email di reimpostazione password all'indirizzo dell'utente corrente. */
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
