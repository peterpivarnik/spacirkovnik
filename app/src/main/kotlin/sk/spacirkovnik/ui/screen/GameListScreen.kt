package sk.spacirkovnik.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.AmberLight
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.GameBackground
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.SecondaryButton
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.GameListViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel.DownloadStatus

@Composable
fun GameListScreen(
    onGameSelected: (String) -> Unit,
    gameListViewModel: GameListViewModel = viewModel()
) {
    val state by gameListViewModel.state

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(16.dp)
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
                    color = AmberLight,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column {
                    Text(
                        text = "Spacirkovnik",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextOnDark,
                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                    )
                    Text(
                        text = "Vyber si dobrodružstvo",
                        fontSize = 16.sp,
                        color = TextOnDark.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.games) { gameWithStatus ->
                            GameCard(
                                gameWithStatus = gameWithStatus,
                                onPlay = { onGameSelected(gameWithStatus.info.id) },
                                onDownload = { gameListViewModel.downloadGame(gameWithStatus.info.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    gameWithStatus: GameListViewModel.GameWithStatus,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val info = gameWithStatus.info

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = info.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = info.description,
                fontSize = 14.sp,
                color = TextMedium,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (info.region != null) {
                    Text(text = info.region, fontSize = 12.sp, color = TextMedium)
                }
                if (info.estimatedDurationMinutes != null) {
                    Text(text = "${info.estimatedDurationMinutes} min", fontSize = 12.sp, color = TextMedium)
                }
                if (info.distanceKm != null) {
                    Text(text = "${info.distanceKm} km", fontSize = 12.sp, color = TextMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (gameWithStatus.status) {
                DownloadStatus.NOT_DOWNLOADED -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryButton)
                    ) {
                        Text("Stiahnuť", color = PrimaryButtonText, fontSize = 16.sp)
                    }
                }
                DownloadStatus.DOWNLOADING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sťahujem...", fontSize = 16.sp)
                    }
                }
                DownloadStatus.DOWNLOADED -> {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                    ) {
                        Text("Hrať", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DownloadStatus.UPDATE_AVAILABLE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPlay,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                        ) {
                            Text("Hrať", color = PrimaryButtonText, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryButton)
                        ) {
                            Text("Aktualizovať", color = PrimaryButtonText)
                        }
                    }
                }
            }
        }
    }
}
