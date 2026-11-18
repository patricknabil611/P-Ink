package com.example.data.cloud

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredentials : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class GoogleAuthHelper(
    private val auth: FirebaseAuth = Firebase.auth,
) {
    val authState: Flow<FirebaseUser?> = auth.authStateFlow()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInInteractive(activityContext: Context): GoogleSignInResult {
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val credentialManager = CredentialManager.create(activityContext)
            val response = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
            exchangeGoogleCredential(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Interactive Google Sign-In cancelled", e)
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available", e)
            GoogleSignInResult.NoCredentials
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleSignInResult.Failure(e.localizedMessage ?: "Google Sign-In failed")
        }
    }

    suspend fun trySilentSignIn(activityContext: Context): GoogleSignInResult {
        if (auth.currentUser != null) {
            return GoogleSignInResult.Success(auth.currentUser!!)
        }
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val credentialManager = CredentialManager.create(activityContext)
            val response = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
            exchangeGoogleCredential(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google Sign-In skipped: ${e.javaClass.simpleName}")
            GoogleSignInResult.NoCredentials
        }
    }

    private suspend fun exchangeGoogleCredential(credential: androidx.credentials.Credential): GoogleSignInResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user ?: return GoogleSignInResult.Failure("Signed-in user was empty.")
            return GoogleSignInResult.Success(user)
        }
        return GoogleSignInResult.Failure("Unsupported credential type returned.")
    }

    suspend fun signOut(appContext: Context) {
        withContext(NonCancellable) {
            runCatching {
                CredentialManager.create(appContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { e ->
                Log.w(TAG, "Failed to clear credential state", e)
            }
            auth.signOut()
        }
    }

    private companion object {
        const val TAG = "GoogleAuthHelper"
    }
}
