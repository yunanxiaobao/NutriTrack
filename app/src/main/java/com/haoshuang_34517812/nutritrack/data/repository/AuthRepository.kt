package com.haoshuang_34517812.nutritrack.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown when a requested username handle is already registered. */
class UsernameTakenException : Exception("This username is already taken")

/**
 * Firebase-backed account operations: registration (with a unique username
 * handle claimed in Firestore), login, logout and password reset.
 *
 * Identity lives in Firebase Authentication; the public profile lives in
 * Firestore under users/{uid}, and usernames/{usernameLower} reserves the
 * handle so two accounts can never share one.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Stream of the signed-in user; emits null when signed out. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Creates a new account. The username is claimed atomically in the same
     * Firestore transaction that writes the profile; if the claim fails the
     * just-created Auth user is rolled back so no orphan account remains.
     */
    suspend fun register(username: String, email: String, password: String): Result<FirebaseUser> {
        val credential = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val user = credential.user
            ?: return Result.failure(IllegalStateException("Account creation returned no user"))

        try {
            val usernameKey = username.lowercase()
            val usernameRef = firestore.collection("usernames").document(usernameKey)
            val userRef = firestore.collection("users").document(user.uid)
            firestore.runTransaction { transaction ->
                if (transaction.get(usernameRef).exists()) {
                    throw UsernameTakenException()
                }
                transaction.set(usernameRef, mapOf("uid" to user.uid))
                transaction.set(
                    userRef,
                    mapOf(
                        "username" to username,
                        "displayName" to username,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "onboardingDone" to false
                    )
                )
                null
            }.await()
        } catch (e: Exception) {
            // Roll back the Auth account so the email is not left half-registered.
            runCatching { user.delete().await() }
            var cause: Throwable = e
            while (cause !is UsernameTakenException && cause.cause != null && cause.cause !== cause) {
                cause = cause.cause!!
            }
            return Result.failure(if (cause is UsernameTakenException) cause else e)
        }

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(username).build()
            ).await()
        }
        // Verification is offered but not enforced in v1 (see account design).
        runCatching { user.sendEmailVerification().await() }
        return Result.success(user)
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
                ?: return Result.failure(IllegalStateException("Login returned no user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    /**
     * Permanently deletes the signed-in account (Play account-deletion
     * requirement): re-authenticates with [currentPassword], deletes the
     * Firestore data (tips, questionnaire, profile), releases the username
     * handle, and finally deletes the Firebase Auth user itself.
     */
    suspend fun deleteAccount(currentPassword: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No signed-in user"))
        val email = user.email
            ?: return Result.failure(IllegalStateException("Account has no email"))
        return try {
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            val uid = user.uid
            val userDoc = firestore.collection("users").document(uid)
            val profile = runCatching { userDoc.get().await() }.getOrNull()
            val username = profile?.getString("username")

            // Firestore does not cascade deletes: clear subcollections first,
            // in batches under the 500-write limit.
            val tips = userDoc.collection("tips").get().await()
            for (chunk in tips.documents.chunked(400)) {
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            val cleanup = firestore.batch()
            cleanup.delete(userDoc.collection("questionnaire").document("current"))
            cleanup.delete(userDoc)
            if (!username.isNullOrBlank()) {
                cleanup.delete(firestore.collection("usernames").document(username.lowercase()))
            }
            cleanup.commit().await()

            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
