package sk.spacirkovnik.ui.component

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
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
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.GameDataViewModel
import sk.spacirkovnik.viewmodel.LocationViewModel

@Composable
fun QuestionsDisplay(
    gameDataViewModel: GameDataViewModel,
    locationViewModel: LocationViewModel? = null,
    context: Context? = null,
    onGameFinished: () -> Unit = {}
) {
    gameDataViewModel.state.value.finished?.let { summary ->
        FinishScreen(summary = summary, onDone = onGameFinished)
        return
    }

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
    var imageLoadFailed by remember(currentScreen.imageUrl) { mutableStateOf(false) }

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
                    if (imageLoadFailed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(Amber.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = Amber,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Načítanie obrázka vyžaduje pripojenie na internet",
                                    fontSize = 13.sp,
                                    color = TextMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = currentScreen.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.FillWidth,
                            onError = { imageLoadFailed = true }
                        )
                    }
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
                            gameDataViewModel.finishGame()
                        } else {
                            gameDataViewModel.incrementIndex()
                        }
                    },
                    currentScreen.buttonText ?: "Ďalej"
                )
                ScreenType.BROWSE -> TwoButtons(
                    backButtonEnabled = gameDataViewModel.getCurrentIndex() > 0,
                    backButtonOnClick = { gameDataViewModel.decrementIndex() },
                    backButtonText = currentScreen.backButtonText ?: "Späť",
                    nextButtonEnabled = gameDataViewModel.getCurrentIndex() < gameDataViewModel.getScreenCount() - 1,
                    nextButtonOnClick = { gameDataViewModel.incrementIndex() },
                    nextButtonText = currentScreen.nextButtonText ?: "Ďalej"
                )
                // key: každá otázka má vlastný stav, aj keď majú dve za sebou rovnaké odpovede.
                ScreenType.QUESTION -> key(gameDataViewModel.getCurrentIndex()) {
                    QuestionWithAnswers(
                        answers = currentScreen.answers ?: emptyList(),
                        onCorrectChoice = { gameDataViewModel.incrementIndex() }
                    )
                }
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
            .defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
    ) {
        Text(text = buttonText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = PrimaryButtonText, textAlign = TextAlign.Center)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            enabled = backButtonEnabled,
            onClick = backButtonOnClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BackButton,
                contentColor = BackButtonText,
                disabledContainerColor = DisabledButton
            )
        ) {
            Text(text = backButtonText, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Button(
            enabled = nextButtonEnabled,
            onClick = nextButtonOnClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButton,
                contentColor = PrimaryButtonText,
                disabledContainerColor = DisabledButton
            )
        ) {
            Text(text = nextButtonText, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QuestionWithAnswers(answers: List<GameAnswer>, onCorrectChoice: () -> Unit) {
    val context = LocalContext.current
    // Stav viažeme na otázku: dve otázky za sebou by inak zdieľali stav predchádzajúcej.
    var correctAnswer by remember(answers) { mutableStateOf<GameAnswer?>(null) }
    val wrongAnswers = remember(answers) { mutableStateListOf<GameAnswer>() }

    // Správna odpoveď: tlačidlo na chvíľu zozelenie s „✓", telefón krátko zavibruje
    // a až potom sa ide ďalej — hráč (dieťa) vidí, že trafil.
    LaunchedEffect(correctAnswer) {
        if (correctAnswer == null) return@LaunchedEffect
        Haptics.correct(context)
        delay(CORRECT_ANSWER_PAUSE_MS)
        onCorrectChoice()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zlá odpoveď nevyskakuje v okne, ktoré treba zatvárať: tlačidlo sa zatrasie, ostane
        // označené „✗" a nad odpoveďami sa objaví povzbudenie. Skúšať sa dá hneď ďalej.
        if (wrongAnswers.isNotEmpty() && correctAnswer == null) {
            Text(
                text = "Tadiaľto cesta nevedie — skús inú odpoveď!",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        answers.forEach { answer ->
            AnswerButton(
                text = answer.text,
                state = when {
                    correctAnswer == answer -> AnswerState.CORRECT
                    answer in wrongAnswers -> AnswerState.WRONG
                    else -> AnswerState.OPEN
                },
                onClick = {
                    if (correctAnswer != null) return@AnswerButton // už ideme ďalej
                    if (answer.correct) {
                        correctAnswer = answer
                    } else {
                        wrongAnswers.add(answer)
                        Haptics.wrong(context)
                    }
                }
            )
        }
    }
}

private enum class AnswerState { OPEN, CORRECT, WRONG }

@Composable
private fun AnswerButton(text: String, state: AnswerState, onClick: () -> Unit) {
    // Zatrasenie pri zlej odpovedi — ako „nie" hlavou.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state != AnswerState.WRONG) return@LaunchedEffect
        for (x in listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)) {
            shake.animateTo(x, animationSpec = tween(durationMillis = 45))
        }
    }
    Button(
        onClick = onClick,
        // Zlú odpoveď už znova neťukne; ostane viditeľná, aby bolo jasné, čo už skúšal.
        enabled = state != AnswerState.WRONG,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (state == AnswerState.CORRECT) CorrectGreen else SecondaryButton,
            contentColor = if (state == AnswerState.CORRECT) Color.White else SecondaryButtonText,
            disabledContainerColor = WrongRed.copy(alpha = 0.8f),
            disabledContentColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = when (state) {
                AnswerState.CORRECT -> "✓ Správne! $text"
                AnswerState.WRONG -> "✗ $text"
                AnswerState.OPEN -> text
            },
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
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
            color = TextOnDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSelected(Gender.MALE) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
        ) {
            Text("🧒 Chlapec", fontSize = 18.sp, color = PrimaryButtonText, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSelected(Gender.FEMALE) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
        ) {
            Text("👧 Dievča", fontSize = 18.sp, color = PrimaryButtonText, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Ako dlho ostane správna odpoveď zelená, kým sa hra posunie ďalej. */
private const val CORRECT_ANSWER_PAUSE_MS = 900L

private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFB3261E)
