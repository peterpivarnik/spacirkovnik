package sk.spacirkovnik.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.spacirkovnik.ui.screen.AutoSizeText
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextOnBeige
import sk.spacirkovnik.ui.theme.TextOnBeigeSecondary
import sk.spacirkovnik.viewmodel.AuthViewModel

private enum class AuthMode { LOGIN, REGISTER, RESET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    state: AuthViewModel.AuthState,
    onDismiss: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailRegister: (String, String) -> Unit,
    onPasswordReset: (String) -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun switchMode(target: AuthMode) {
        mode = target
        confirmPassword = ""
        localError = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (mode) {
                    AuthMode.LOGIN -> "Prihlásenie"
                    AuthMode.REGISTER -> "Registrácia"
                    AuthMode.RESET -> "Obnova hesla"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnBeige
            )

            if (mode == AuthMode.RESET) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Zadaj svoj email a pošleme ti odkaz na nastavenie nového hesla.",
                    fontSize = 13.sp,
                    color = TextOnBeigeSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // --- Google navrchu (mimo režimu obnovy) ---
            if (mode != AuthMode.RESET) {
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onGoogleSignIn,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                ) {
                    AutoSizeText(
                        text = "Prihlásiť sa pomocou Google",
                        color = PrimaryButtonText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = TextOnBeigeSecondary.copy(alpha = 0.3f))
                    Text(
                        text = "alebo",
                        fontSize = 13.sp,
                        color = TextOnBeigeSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = TextOnBeigeSecondary.copy(alpha = 0.3f))
                }
            }

            // --- Email / heslo formulár ---
            Spacer(Modifier.height(if (mode == AuthMode.RESET) 20.dp else 16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode != AuthMode.RESET) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text("Heslo") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Skryť heslo" else "Zobraziť heslo",
                                tint = TextOnBeigeSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (mode == AuthMode.REGISTER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = null },
                    label = { Text("Zopakuj heslo") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val shownError = localError ?: state.error
            if (shownError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = shownError,
                    fontSize = 13.sp,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.info != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.info,
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when (mode) {
                        AuthMode.LOGIN -> onEmailSignIn(email, password)
                        AuthMode.REGISTER ->
                            if (password != confirmPassword) localError = "Heslá sa nezhodujú."
                            else onEmailRegister(email, password)
                        AuthMode.RESET -> onPasswordReset(email)
                    }
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = PrimaryButtonText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN -> "Prihlásiť sa"
                            AuthMode.REGISTER -> "Zaregistrovať sa"
                            AuthMode.RESET -> "Poslať email na obnovu"
                        },
                        color = PrimaryButtonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            when (mode) {
                AuthMode.LOGIN -> {
                    TextButton(onClick = { switchMode(AuthMode.RESET) }) {
                        Text("Zabudnuté heslo?", color = TextOnBeigeSecondary, fontSize = 14.sp)
                    }
                    TextButton(onClick = { switchMode(AuthMode.REGISTER) }) {
                        Text(
                            text = "Nemáš účet? Zaregistruj sa",
                            color = Amber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                AuthMode.REGISTER -> {
                    TextButton(onClick = { switchMode(AuthMode.LOGIN) }) {
                        Text(
                            text = "Máš už účet? Prihlás sa",
                            color = Amber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                AuthMode.RESET -> {
                    TextButton(onClick = { switchMode(AuthMode.LOGIN) }) {
                        Text("Späť na prihlásenie", color = TextOnBeigeSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
