package com.example.spacirkovnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement.Bottom
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spacirkovnik.ui.theme.SpacirkovnikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpacirkovnikTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Spacirkovnik(innerPadding)
                }
            }
        }
    }
}

@Composable
fun Spacirkovnik(innerPadding: PaddingValues) {
    val gameDataViewModel: GameDataViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val holdersState by gameDataViewModel.holdersState
    Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
        when {
            holdersState.loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            holdersState.error != null -> {
                Text("ERROR OCCURRED")
            }
            else -> {
                AppBody(gameDataViewModel, locationViewModel)
            }
        }
    }
}

@Composable
private fun AppBody(gameDataViewModel: GameDataViewModel, locationViewModel: LocationViewModel) {
    Column(modifier = Modifier
            .fillMaxSize()
            .paint(painterResource(id = R.drawable.forrester2), contentScale = Crop),
           verticalArrangement = Bottom,
           horizontalAlignment = CenterHorizontally) {
        val context = LocalContext.current
        Column(modifier = Modifier.fillMaxSize(),
               horizontalAlignment = CenterHorizontally,
               verticalArrangement = Center) {
            LocationDisplay(locationViewModel = locationViewModel, context = context)
            QuestionsDisplay(gameDataViewModel)
        }
    }
}