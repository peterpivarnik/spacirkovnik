package sk.spacirkovnik.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.spacirkovnik.ui.component.QuestionsDisplay
import sk.spacirkovnik.ui.theme.Amber
import androidx.compose.ui.graphics.Color
import sk.spacirkovnik.ui.theme.GameBackground
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.GameDataViewModel
import sk.spacirkovnik.viewmodel.LocationViewModel

@Composable
fun GamePlayScreen(
    gameId: String,
    onExit: () -> Unit = {},
    gameDataViewModel: GameDataViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    LaunchedEffect(gameId) {
        gameDataViewModel.loadGame(gameId)
    }

    val state by gameDataViewModel.state
    var showExitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp)
        ) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Ukončiť hru",
                    tint = TextOnDark
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Amber
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = TextOnDark
                    )
                }
                else -> {
                    val context = LocalContext.current
                    QuestionsDisplay(gameDataViewModel, locationViewModel, context, onGameFinished = onExit)
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("Ukončiť hru", fontWeight = FontWeight.Bold, color = TextDark)
            },
            text = {
                Text("Chceš si uložiť postup a pokračovať neskôr, alebo začať odznova?", color = TextMedium)
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onExit()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                    ) {
                        Text("Ukončiť a uložiť", color = PrimaryButtonText, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            gameDataViewModel.clearProgress()
                            onExit()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text("Ukončiť a vymazať postup", color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        "Pri vymazaní postupu budete musieť začať hrať hru odznova!",
                        fontSize = 12.sp,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showExitDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zrušiť", color = TextMedium)
                    }
                }
            }
        )
    }
}
