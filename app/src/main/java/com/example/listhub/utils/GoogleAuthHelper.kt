package com.example.listhub.utils

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps the Google Sign-In setup needed for two things:
 *  1) Signing in to Firebase Auth via Google (SSO), and
 *  2) Getting an OAuth access token scoped for the Google Tasks API, so we
 *     can make real authenticated calls to https://tasks.googleapis.com/.
 *
 * NOTE: for Google Sign-In to actually work you need to register your
 * debug/release SHA-1 fingerprint against your Firebase project (see the
 * README) - otherwise you'll get a DEVELOPER_ERROR / status code 10.
 */
object GoogleAuthHelper {

    // scope needed to read/write the signed-in user's Google Tasks
    private const val TASKS_SCOPE = "oauth2:https://www.googleapis.com/auth/tasks"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.listhub.R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/tasks"))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    /**
     * Fetches a fresh OAuth access token for the Tasks API scope. This makes a
     * blocking network call under the hood, so it MUST be called off the main
     * thread - we wrap it in Dispatchers.IO here to make that hard to forget.
     */
    suspend fun getTasksApiAccessToken(context: Context, account: GoogleSignInAccount): String {
        return withContext(Dispatchers.IO) {
            val androidAccount = Account(account.email, "com.google")
            GoogleAuthUtil.getToken(context, androidAccount, TASKS_SCOPE)
        }
    }
}
