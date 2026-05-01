package com.neoconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neoconnect.app.ui.screens.CallScreen
import com.neoconnect.app.ui.screens.HomeScreen
import com.neoconnect.app.ui.screens.SplashScreen
import com.neoconnect.app.ui.theme.NeoConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // এই লাইনটি super.onCreate এর আগে হতে হবে
        val splashScreen = installSplashScreen()        
        super.onCreate(savedInstanceState)
        
        setContent {
            NeoConnectTheme {
                val navController = rememberNavController()
                SetupNavigation(navController)
            }
        }
    }
}

@Composable
fun SetupNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen { roomId, isVideoCall ->
                navController.navigate("call/$roomId/$isVideoCall")
            }
        }
        composable("call/{roomId}/{isVideoCall}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val isVideoCall = backStackEntry.arguments?.getString("isVideoCall")?.toBoolean() ?: true
            
            CallScreen(roomId = roomId, isVideoCall = isVideoCall) {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
    }
}