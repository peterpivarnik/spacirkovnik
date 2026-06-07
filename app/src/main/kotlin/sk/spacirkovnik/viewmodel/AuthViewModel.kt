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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
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

    // Account linking: keď email koliduje s iným providerom, podržíme credential
    // a prepojíme ho po prihlásení existujúcim spôsobom (jeden UID = jedny aktivácie).
    private var pendingEmailCredential: AuthCredential? = null
    private var pendingGoogleCredential: AuthCredential? = null

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _state.value = AuthState(
                isSignedIn = true,
                userName = currentUser.displayName ?: currentUser.email?.substringBefore("@"),
                userEmail = currentUser.email,
                loading = true
            )
            loadActivations()
            loadTestGames()
            loadConsents()
        }
    }

    // --- Google Sign-In ---

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
                _state.value = _state.value.copy(loading = true, error = null, info = null)
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                    ?: throw Exception("ID token je null")

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                try {
                    val authResult = withTimeout(15_000) {
                        auth.signInWithCredential(credential).await()
                    }
                    // Direction A: po Google prihlásení prepoj čakajúce email/heslo
                    pendingEmailCredential?.let { cred ->
                        authResult.user?.linkWithCredential(cred)?.await()
                        pendingEmailCredential = null
                    }
                    finishSignIn(authResult.user)
                } catch (_: FirebaseAuthUserCollisionException) {
                    // Direction B: email už existuje s heslom — podrž Google a vyzvi na heslo
                    pendingGoogleCredential = credential
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Tento email už používaš s heslom. Prihlás sa emailom a heslom — Google sa potom prepojí."
                    )
                }
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

    // --- Email / heslo ---

    fun signInWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, error = null, info = null)
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                // Direction B: po prihlásení heslom prepoj čakajúci Google credential
                pendingGoogleCredential?.let { cred ->
                    result.user?.linkWithCredential(cred)?.await()
                    pendingGoogleCredential = null
                }
                finishSignIn(result.user)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = mapError(e))
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, error = null, info = null)
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                result.user?.sendEmailVerification()
                finishSignIn(result.user)
            } catch (_: FirebaseAuthUserCollisionException) {
                // Direction A: email už existuje cez Google — podrž heslo a vyzvi na Google
                pendingEmailCredential = EmailAuthProvider.getCredential(email.trim(), password)
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Tento email už používaš cez Google. Prihlás sa cez Google a heslo sa prepojí."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = mapError(e))
            }
        }
    }

    fun sendPasswordReset(email: String) {
        Log.d("AuthVM", "sendPasswordReset spustený, email='${email}'")
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Zadaj email pre obnovu hesla.")
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                Log.d("AuthVM", "Reset email odoslaný OK")
                _state.value = _state.value.copy(
                    error = null,
                    info = "Odoslali sme ti email na obnovu hesla. Skontroluj aj spam."
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Reset zlyhal: ${e::class.simpleName}: ${e.message}")
                _state.value = _state.value.copy(error = mapError(e))
            }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Vyplň email aj heslo.")
            return false
        }
        return true
    }

    private fun finishSignIn(user: FirebaseUser?) {
        _state.value = AuthState(
            isSignedIn = true,
            userName = user?.displayName ?: user?.email?.substringBefore("@"),
            userEmail = user?.email
        )
        loadActivations()
        loadTestGames()
        loadConsents()
    }

    private fun mapError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Heslo musí mať aspoň 6 znakov."
        is FirebaseAuthInvalidUserException -> "Účet s týmto emailom neexistuje. Zaregistruj sa nižšie."
        is FirebaseAuthInvalidCredentialsException -> "Nesprávny email alebo heslo. Ak ešte nemáš účet, zaregistruj sa nižšie."
        is FirebaseAuthUserCollisionException -> "Tento email už je zaregistrovaný."
        else -> "Prihlásenie zlyhalo: ${e.message}"
    }

    @Suppress("DEPRECATION")
    fun signOut() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(getApplication(), gso).signOut()
        pendingEmailCredential = null
        pendingGoogleCredential = null
        _state.value = AuthState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, info = null)
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

    private fun loadConsents() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = database.reference
                    .child("consents")
                    .child(uid)
                    .get()
                    .await()

                val consents = mutableMapOf<String, Int>()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val version = when (val v = child.child("version").value) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> return@forEach
                    }
                    consents[key] = version
                }
                _state.value = _state.value.copy(consents = consents)
            } catch (_: Exception) {
                // consents stay empty on error; local ConsentManager cache still applies
            }
        }
    }

    /** True if the user already accepted [gameId] consent at [version] or newer. */
    fun hasConsent(gameId: String, version: Int): Boolean {
        return (_state.value.consents[gameId] ?: -1) >= version
    }

    /**
     * Records the user's consent for [gameId] at [version] in Firebase, along with the e-mail.
     * [onResult] is called with true only after the server write succeeds, so the caller can
     * start the game (and cache locally) only once the consent is safely stored.
     */
    fun recordConsent(gameId: String, version: Int, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val record = mapOf(
                    "version" to version,
                    "acceptedAt" to System.currentTimeMillis(),
                    "email" to (user.email ?: "")
                )
                database.reference
                    .child("consents")
                    .child(user.uid)
                    .child(gameId)
                    .setValue(record)
                    .await()
                val updated = _state.value.consents.toMutableMap().also { it[gameId] = version }
                _state.value = _state.value.copy(consents = updated)
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
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
        val consents: Map<String, Int> = emptyMap(),
        val loading: Boolean = false,
        val error: String? = null,
        val info: String? = null
    )
}
