package com.example.spacirkovnik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun QuestionsDisplay(gameDataViewModel: GameDataViewModel) {
    TextField(value = gameDataViewModel.getCurrentDataHolder().displayText,
              onValueChange = {},
              modifier = Modifier.padding(vertical = 4.dp),
              shape = RoundedCornerShape(8.dp),
              enabled = true,
              readOnly = true,
              textStyle = TextStyle.Default.copy(fontSize = gameDataViewModel.getCurrentDataHolder().fontSize),
              colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xffd8e6ff),
                                                        focusedContainerColor = Color(0xffd8e6ff)))
    when (gameDataViewModel.getCurrentDataHolder().componentType) {
        ComponentType.ONE_BUTTON -> OneButton({ gameDataViewModel.incrementIndex() },
                                              gameDataViewModel.getCurrentDataHolder().firstButtonText)
        ComponentType.TWO_BUTTONS -> TwoButtons(gameDataViewModel.getCurrentIndex() > 0,
                                                { gameDataViewModel.decrementIndex() },
                                                gameDataViewModel.getCurrentDataHolder().firstButtonText,
                                                gameDataViewModel.getCurrentIndex() < gameDataViewModel.getHoldersListSize() - 1,
                                                { gameDataViewModel.incrementIndex() },
                                                gameDataViewModel.getCurrentDataHolder().secondButtonText)
        ComponentType.QUESTION -> QuestionWithAnswers(answers = gameDataViewModel.getCurrentDataHolder().answers,
                                                      onCorrectChoice = { gameDataViewModel.incrementIndex() })
    }
}

@Composable
private fun OneButton(buttonOnClick: () -> Unit, buttonText: String) {
    Button(onClick = buttonOnClick) {
        Text(text = buttonText)
    }
}

@Composable
private fun TwoButtons(backButtonEnabled: Boolean,
                       backButtonOnClick: () -> Unit,
                       backButtonText: String,
                       nextButtonEnabled: Boolean,
                       nextButtonOnClick: () -> Unit,
                       nextButtonText: String) {
    Row(horizontalArrangement = Arrangement.SpaceAround) {
        Button(enabled = backButtonEnabled, onClick = backButtonOnClick) {
            Text(text = backButtonText)
        }
        Spacer(modifier = Modifier.padding(25.dp))
        Button(enabled = nextButtonEnabled, onClick = nextButtonOnClick) {
            Text(text = nextButtonText)
        }
    }
}

@Composable
private fun QuestionWithAnswers(answers: List<Answer>, onCorrectChoice: () -> Unit) {
    val showAlertMessage = remember { mutableStateOf(false) }
    if (showAlertMessage.value) {
        MyAlertDialog(showAlertMessage)
    }
    answers.forEach { action ->
        Button(
            onClick = {
                if (action.valid) {
                    onCorrectChoice.invoke()
                }
                else {
                    showAlertMessage.value = true
                }
            },
            modifier = Modifier.width(256.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = true) {
            Text(text = action.answer)
        }
    }
}

@Composable
private fun MyAlertDialog(showAlertMessage: MutableState<Boolean>) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Warning,
                 contentDescription = "Example Icon")
        },
        title = {
            Text(text = "Zlá odpoveď")
        },
        text = {
            Text(text = "Skontroluj si poriadne svoju odpoveď")
        },
        onDismissRequest = {
            showAlertMessage.value = false
        },
        confirmButton = {
            TextButton(onClick = { showAlertMessage.value = false })
            { Text("OK") }
        }
    )
}