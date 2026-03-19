package sk.spacirkovnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import sk.spacirkovnik.ui.screen.GameListScreen
import sk.spacirkovnik.ui.screen.GamePlayScreen
import sk.spacirkovnik.ui.theme.SpacirkovnikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpacirkovnikTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "gameList",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("gameList") {
                            GameListScreen(
                                onGameSelected = { gameId ->
                                    navController.navigate("gamePlay/$gameId")
                                }
                            )
                        }
                        composable("gamePlay/{gameId}") { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                            GamePlayScreen(gameId = gameId)
                        }
                    }
                }
            }
        }
    }
}
