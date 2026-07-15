package com.zobaze.parkspot.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/** Thin wrapper over Firebase email/password auth. */
class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user ?: error("Sign-in returned no user")
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user ?: error("Sign-up returned no user")
    }

    fun signOut() = auth.signOut()
}
