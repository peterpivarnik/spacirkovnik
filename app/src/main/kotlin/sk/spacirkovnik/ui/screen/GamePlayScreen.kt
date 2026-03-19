package sk.spacirkovnik.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.spacirkovnik.ui.component.QuestionsDisplay
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.GameBackground
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.GameDataViewModel
import sk.spacirkovnik.viewmodel.LocationViewModel

@Composable
fun GamePlayScreen(
    gameId: String,
    gameDataViewModel: GameDataViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    LaunchedEffect(gameId) {
        gameDataViewModel.loadGame(gameId)
    }

    val state by gameDataViewModel.state

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
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
                QuestionsDisplay(gameDataViewModel, locationViewModel, context)
            }
        }
    }
}
