package info.hkdevstudio.gom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.screen.MapScreen
import info.hkdevstudio.gom.ui.screen.RecordsScreen
import info.hkdevstudio.gom.ui.theme.GomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GomTheme {
                GomApp()
            }
        }
    }
}

@Composable
fun GomApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            MapScreen(
                viewModel = viewModel,
                onOpenRecords = { navController.navigate("records") },
            )
        }
        composable("records") {
            RecordsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
