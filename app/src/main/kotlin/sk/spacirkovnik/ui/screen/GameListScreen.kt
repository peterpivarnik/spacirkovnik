package sk.spacirkovnik.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import sk.spacirkovnik.R
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.AmberLight
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.MainBackground
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.SecondaryButton
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.AuthViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel.DownloadStatus

@Composable
fun GameListScreen(
    onGameSelected: (String) -> Unit,
    gameListViewModel: GameListViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val state by gameListViewModel.state
    val authState by authViewModel.state
    val activity = LocalActivity.current!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Spacirkovnik logo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "Špacírkovník",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextOnDark,
                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                    )
                    Text(
                        text = "Vyber si dobrodružstvo",
                        fontSize = 16.sp,
                        color = TextOnDark.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (authState.isSignedIn) {
                        Text(
                            text = authState.userName ?: authState.userEmail ?: "",
                            fontSize = 13.sp,
                            color = TextOnDark.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = { authViewModel.signOut() }) {
                            Text("Odhlásiť sa", fontSize = 12.sp, color = TextOnDark.copy(alpha = 0.5f))
                        }
                    } else {
                        Button(
                            onClick = { authViewModel.signIn(activity) },
                            enabled = !authState.loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                        ) {
                            Text(
                                if (authState.loading) "Prihlasujem..." else "Prihlásiť sa",
                                color = PrimaryButtonText,
                                fontSize = 14.sp
                            )
                        }
                        if (authState.error != null) {
                            Text(
                                text = authState.error!!,
                                fontSize = 12.sp,
                                color = AmberLight,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.games) { gameWithStatus ->
                            val isActivated = authViewModel.isGameActivated(gameWithStatus.info.id)
                            GameCard(
                                gameWithStatus = gameWithStatus,
                                isActivated = isActivated,
                                onPlay = { onGameSelected(gameWithStatus.info.id) },
                                onDownload = { gameListViewModel.downloadGame(gameWithStatus.info.id) }
                            )
                        }
                    }
                }
            }
        } // Column padding(20.dp)
    } // Column outer
}

@Composable
private fun GameCard(
    gameWithStatus: GameListViewModel.GameWithStatus,
    isActivated: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val info = gameWithStatus.info
    val isLocked = info.playable == false && !isActivated

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLocked) Modifier.border(
                    width = 1.dp,
                    color = TextMedium.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) CardBg.copy(alpha = 0.5f) else CardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .then(if (isLocked) Modifier.alpha(0.5f) else Modifier)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = info.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = info.description,
                        fontSize = 15.sp,
                        color = TextMedium,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!info.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = info.imageUrl,
                        contentDescription = info.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = TextMedium.copy(alpha = 0.2f)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (info.region != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = info.region, fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                    }
                }
                if (info.estimatedDurationMinutes != null || info.distanceKm != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (info.estimatedDurationMinutes != null) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${info.estimatedDurationMinutes} min", fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                        }
                        if (info.estimatedDurationMinutes != null && info.distanceKm != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "•", fontSize = 12.sp, color = TextMedium)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (info.distanceKm != null) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${info.distanceKm} km", fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                        }
                    }
                }
                if (info.startName != null || info.endName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${info.startName ?: ""} → ${info.endName ?: ""}", fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextMedium,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vyžaduje aktiváciu", fontSize = 14.sp, color = TextMedium)
                }
            } else {
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
}
