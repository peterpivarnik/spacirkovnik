package sk.spacirkovnik.ui.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import sk.spacirkovnik.model.GameAnswer
import sk.spacirkovnik.model.Gender
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.model.ScreenType
import sk.spacirkovnik.model.applyGender
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.BackButton
import sk.spacirkovnik.ui.theme.BackButtonText
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.DisabledButton
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.SecondaryButton
import sk.spacirkovnik.ui.theme.SecondaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.viewmodel.GameDataViewModel
import sk.spacirkovnik.viewmodel.LocationViewModel

@Composable
fun QuestionsDisplay(
    gameDataViewModel: GameDataViewModel,
    locationViewModel: LocationViewModel? = null,
    context: Context? = null,
    onGameFinished: () -> Unit = {}
) {
    val gender by gameDataViewModel.gender
    val resolvedGender = gender

    if (resolvedGender == null) {
        GenderSelectionScreen(onSelected = { gameDataViewModel.setGender(it) })
        return
    }

    val currentScreen = gameDataViewModel.getCurrentScreen()
    val processedText = currentScreen.text?.applyGender(resolvedGender) ?: ""

    if (currentScreen.type == ScreenType.NAVIGATION
        && locationViewModel != null && context != null
    ) {
        val prevNavLocation = (gameDataViewModel.getCurrentIndex() - 1 downTo 0)
            .map { gameDataViewModel.state.value.screens[it] }
            .firstOrNull { it.targetLatitude != null && it.targetLongitude != null }
            ?.let { LocationData(it.targetLatitude!!, it.targetLongitude!!) }

        NavigationDisplay(
            gameScreen = currentScreen.copy(text = processedText),
            locationViewModel = locationViewModel,
            context = context,
            onContinue = { gameDataViewModel.incrementIndex() },
            debugStartLocation = prevNavLocation
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!currentScreen.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentScreen.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
                Text(
                    text = processedText,
                    fontSize = (currentScreen.fontSize ?: 18).sp,
                    lineHeight = ((currentScreen.fontSize ?: 18) + 8).sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if ((currentScreen.fontSize ?: 18) > 20) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(20.dp),
                    color = TextDark
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp, top = 20.dp)
        ) {
            when (currentScreen.type) {
                ScreenType.CONTINUE -> OneButton(
                    {
                        if (gameDataViewModel.isLastScreen()) {
                            gameDataViewModel.recordCompletion()
                            gameDataViewModel.clearProgress()
                            onGameFinished()
                        } else {
                            gameDataViewModel.incrementIndex()
                        }
                    },
                    currentScreen.buttonText ?: "Pokračovať"
                )
                ScreenType.BROWSE -> TwoButtons(
                    backButtonEnabled = gameDataViewModel.getCurrentIndex() > 0,
                    backButtonOnClick = { gameDataViewModel.decrementIndex() },
                    backButtonText = currentScreen.backButtonText ?: "Späť",
                    nextButtonEnabled = gameDataViewModel.getCurrentIndex() < gameDataViewModel.getScreenCount() - 1,
                    nextButtonOnClick = { gameDataViewModel.incrementIndex() },
                    nextButtonText = currentScreen.nextButtonText ?: "Pokračovať"
                )
                ScreenType.QUESTION -> QuestionWithAnswers(
                    answers = currentScreen.answers ?: emptyList(),
                    onCorrectChoice = { gameDataViewModel.incrementIndex() }
                )
                ScreenType.NAVIGATION -> {}
                null -> {}
            }
        }
    }
}

@Composable
private fun OneButton(buttonOnClick: () -> Unit, buttonText: String) {
    Button(
        onClick = buttonOnClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
    ) {
        Text(text = buttonText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = PrimaryButtonText)
    }
}

@Composable
private fun TwoButtons(
    backButtonEnabled: Boolean,
    backButtonOnClick: () -> Unit,
    backButtonText: String,
    nextButtonEnabled: Boolean,
    nextButtonOnClick: () -> Unit,
    nextButtonText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            enabled = backButtonEnabled,
            onClick = backButtonOnClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BackButton,
                contentColor = BackButtonText,
                disabledContainerColor = DisabledButton
            )
        ) {
            Text(text = backButtonText, fontSize = 16.sp)
        }
        Button(
            enabled = nextButtonEnabled,
            onClick = nextButtonOnClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButton,
                contentColor = PrimaryButtonText,
                disabledContainerColor = DisabledButton
            )
        ) {
            Text(text = nextButtonText, fontSize = 16.sp)
        }
    }
}

@Composable
private fun QuestionWithAnswers(answers: List<GameAnswer>, onCorrectChoice: () -> Unit) {
    val showAlertMessage = remember { mutableStateOf(false) }
    if (showAlertMessage.value) {
        MyAlertDialog(showAlertMessage)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        answers.forEach { answer ->
            Button(
                onClick = {
                    if (answer.correct) {
                        onCorrectChoice.invoke()
                    } else {
                        showAlertMessage.value = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryButton,
                    contentColor = SecondaryButtonText
                )
            ) {
                Text(text = answer.text, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MyAlertDialog(showAlertMessage: MutableState<Boolean>) {
    AlertDialog(
        onDismissRequest = { showAlertMessage.value = false },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Varovanie",
                tint = Amber,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "Zlá odpoveď",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Skúš to znova, určite to zvládneš!",
                fontSize = 16.sp,
                color = TextMedium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { showAlertMessage.value = false },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
            ) {
                Text("Skúsim znova", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun GenderSelectionScreen(onSelected: (Gender) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Kto sa vydáva na dobrodružstvo?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSelected(Gender.MALE) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
        ) {
            Text("🧒 Chlapec", fontSize = 18.sp, color = PrimaryButtonText, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSelected(Gender.FEMALE) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
        ) {
            Text("👧 Dievča", fontSize = 18.sp, color = PrimaryButtonText, fontWeight = FontWeight.SemiBold)
        }
    }
}
