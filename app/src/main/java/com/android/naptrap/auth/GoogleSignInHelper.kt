package com.android.naptrap.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.android.naptrap.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

class GoogleSignInHelper(private val context: Context) {
    
    private val credentialManager = CredentialManager.create(context)
    
    suspend fun signIn(): String? {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.WEB_CLIENT_ID) // Using BuildConfig for security
                .setFilterByAuthorizedAccounts(false) // Allow any Google account
                .setAutoSelectEnabled(false) // Don't auto-select account
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            credential.idToken
            
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignInHelper", "GetCredentialException: ${e.message}", e)
            null
        } catch (e: CancellationException) {
            Log.d("GoogleSignInHelper", "Sign-in cancelled by user")
            null
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Unexpected error during sign-in: ${e.message}", e)
            null
        }
    }
}
