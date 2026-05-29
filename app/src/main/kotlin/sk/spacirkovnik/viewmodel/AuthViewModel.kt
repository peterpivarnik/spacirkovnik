package sk.spacirkovnik.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// FIXME: GoogleSignIn API je deprecated v prospech androidx.credentials.CredentialManager.
// Dôvod ponechania: CredentialManager zobrazuje sign-in ako priehľadný overlay (SignInCredentialChooserActivity
// s TRANSLUCENT témou), ktorý používateľ na Moto G73 / Android 14 nevidí — appka vyzerá zamrznutá.
// Legacy GoogleSignIn otvára klasický plnohodnotný dialóg výberu účtu, ktorý funguje spoľahlivo.
// Odstrániť keď: Google opraví UX CredentialManager-a (viditeľný bottom sheet / plnohodnotný dialóg)
// alebo keď play-services-auth prestane byť súčasťou Play Services (nepravdepodobné v blízkej budúcnosti).
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
            loadTestGames()
        }
    }

    @Suppress("DEPRECATION")
    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(sk.spacirkovnik.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    @Suppress("DEPRECATION")
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, error = null)
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                    ?: throw Exception("ID token je null")

                Log.d("AuthVM", "Mám idToken, prihlasujem do Firebase...")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = withTimeout(15_000) {
                    auth.signInWithCredential(credential).await()
                }
                val user = authResult.user
                Log.d("AuthVM", "Firebase OK: ${user?.email}")

                _state.value = AuthState(
                    isSignedIn = true,
                    userName = user?.displayName,
                    userEmail = user?.email
                )
                loadActivations()
                loadTestGames()
            } catch (e: ApiException) {
                Log.e("AuthVM", "GoogleSignIn ApiException code=${e.statusCode}: ${e.message}")
                _state.value = _state.value.copy(
                    loading = false,
                    error = if (e.statusCode == 12501) null // user cancelled
                    else "Prihlásenie zlyhalo (${e.statusCode})"
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Chyba prihlásenia: ${e::class.simpleName}: ${e.message}", e)
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
                val snapshot = withTimeout(10_000) {
                    database.reference
                        .child("activations")
                        .child(uid)
                        .get()
                        .await()
                }

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

    private fun loadTestGames() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = database.reference
                    .child("testGames")
                    .child(uid)
                    .get()
                    .await()

                val testGames = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    if (child.getValue(Boolean::class.java) == true) {
                        testGames.add(child.key ?: return@forEach)
                    }
                }
                _state.value = _state.value.copy(testGames = testGames)
            } catch (_: Exception) {
                // testGames stays empty on error
            }
        }
    }

    fun isGameActivated(gameId: String): Boolean {
        return _state.value.activatedGames.contains(gameId)
    }

    fun isTestGame(gameId: String): Boolean {
        return _state.value.testGames.contains(gameId)
    }

    fun grantActivation(gameId: String) {
        val updated = _state.value.activatedGames.toMutableSet().also { it.add(gameId) }
        _state.value = _state.value.copy(activatedGames = updated)
    }

    data class AuthState(
        val isSignedIn: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
        val activatedGames: Set<String> = emptySet(),
        val testGames: Set<String> = emptySet(),
        val loading: Boolean = false,
        val error: String? = null
    )
}
