package sk.spacirkovnik.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import sk.spacirkovnik.data.FIREBASE_DATABASE_URL

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL)

    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _state.value = AuthState(
                isSignedIn = true,
                userName = currentUser.displayName,
                userEmail = currentUser.email,
                loading = true
            )
            loadActivations()
        }
    }

    fun signIn(activityContext: android.app.Activity) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, error = null)

                val credentialManager = CredentialManager.create(activityContext)
                val webClientId = getWebClientId()

                val idToken = try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(activityContext, request)
                    GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                } catch (_: NoCredentialException) {
                    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()

                    val result = credentialManager.getCredential(activityContext, request)
                    GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                }

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                _state.value = AuthState(
                    isSignedIn = true,
                    userName = user?.displayName,
                    userEmail = user?.email
                )
                loadActivations()
            } catch (_: GetCredentialCancellationException) {
                _state.value = _state.value.copy(loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Prihlásenie zlyhalo: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _state.value = AuthState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadActivations() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = database.reference
                    .child("activations")
                    .child(uid)
                    .get()
                    .await()

                val activatedGames = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    if (child.getValue(Boolean::class.java) == true) {
                        activatedGames.add(child.key ?: return@forEach)
                    }
                }
                _state.value = _state.value.copy(
                    activatedGames = activatedGames,
                    loading = false
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun isGameActivated(gameId: String): Boolean {
        return _state.value.activatedGames.contains(gameId)
    }

    private fun getWebClientId(): String {
        val appContext = getApplication<Application>()
        return appContext.getString(sk.spacirkovnik.R.string.default_web_client_id)
    }

    data class AuthState(
        val isSignedIn: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
        val activatedGames: Set<String> = emptySet(),
        val loading: Boolean = false,
        val error: String? = null
    )
}
