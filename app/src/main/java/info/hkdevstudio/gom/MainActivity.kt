package info.hkdevstudio.gom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.screen.KakaoPlaceWebScreen
import info.hkdevstudio.gom.ui.screen.MapScreen
import info.hkdevstudio.gom.ui.screen.OnboardingScreen
import info.hkdevstudio.gom.ui.screen.PlaceDetailScreen
import info.hkdevstudio.gom.ui.screen.RecordsScreen
import info.hkdevstudio.gom.ui.screen.RouletteScreen
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomTheme
import info.hkdevstudio.gom.util.LocationProvider
import kotlinx.coroutines.launch

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

private object Routes {
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val ROULETTE = "roulette"
    const val RECORDS = "records"
    const val PLACE = "place/{placeId}?visitId={visitId}"
    fun place(placeId: String, visitId: Long? = null) = "place/$placeId?visitId=${visitId ?: -1L}"
    const val WEB = "web/{placeId}?title={title}"
    fun web(placeId: String, title: String) = "web/$placeId?title=${android.net.Uri.encode(title)}"
}

@Composable
fun GomApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onboardingDone by viewModel.onboardingDone.collectAsState()

    // 첫 실행 플래그 로드 전에는 크림 배경만(스플래시)
    val done = onboardingDone ?: run {
        // 스플래시: 첫 실행 플래그 로드 동안 워드마크
        Box(Modifier.fillMaxSize().background(Cream), contentAlignment = androidx.compose.ui.Alignment.Center) {
            info.hkdevstudio.gom.ui.screen.Wordmark()
        }
        return
    }
    val start = if (done || LocationProvider.hasPermission(context)) Routes.MAP else Routes.ONBOARDING
    val duration = 250

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { fadeIn(tween(duration)) + slideInHorizontally(tween(duration)) { it / 8 } },
        exitTransition = { fadeOut(tween(duration)) },
        popEnterTransition = { fadeIn(tween(duration)) },
        popExitTransition = { fadeOut(tween(duration)) + slideOutHorizontally(tween(duration)) { it / 8 } },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onStartWithLocation = {
                    viewModel.completeOnboarding()
                    viewModel.refreshFromMyLocation()
                    navController.navigate(Routes.MAP) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
                onStartAtDefault = {
                    viewModel.completeOnboarding()
                    viewModel.startAtDefaultLocation()
                    navController.navigate(Routes.MAP) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.MAP) {
            MapScreen(
                viewModel = viewModel,
                onOpenRecords = { navController.navigate(Routes.RECORDS) },
                onOpenRoulette = { navController.navigate(Routes.ROULETTE) },
                onOpenPlace = { place -> navController.navigate(Routes.place(place.id)) },
            )
        }
        composable(Routes.ROULETTE) {
            RouletteScreen(
                viewModel = viewModel,
                onClose = { navController.popBackStack() },
                onShowOnMap = { place ->
                    viewModel.selectPlace(place.id)
                    navController.popBackStack(Routes.MAP, inclusive = false)
                },
                onDecide = { place ->
                    scope.launch {
                        val visitId = viewModel.recordVisit(place)
                        navController.navigate(Routes.place(place.id, visitId)) {
                            popUpTo(Routes.ROULETTE) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            route = Routes.PLACE,
            arguments = listOf(
                navArgument("placeId") { type = NavType.StringType },
                navArgument("visitId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val placeId = entry.arguments?.getString("placeId").orEmpty()
            val visitId = entry.arguments?.getLong("visitId")?.takeIf { it > 0 }
            PlaceDetailScreen(
                viewModel = viewModel,
                placeId = placeId,
                pendingVisitId = visitId,
                onBack = { navController.popBackStack() },
                onOpenRecords = { navController.navigate(Routes.RECORDS) },
                onOpenKakaoPage = { place -> navController.navigate(Routes.web(place.id, place.name)) },
            )
        }
        composable(
            route = Routes.WEB,
            arguments = listOf(
                navArgument("placeId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val placeId = entry.arguments?.getString("placeId").orEmpty()
            KakaoPlaceWebScreen(
                url = "https://place.map.kakao.com/$placeId",
                title = entry.arguments?.getString("title").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.RECORDS) {
            RecordsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenRoulette = { navController.navigate(Routes.ROULETTE) },
                onOpenPlace = { id -> navController.navigate(Routes.place(id)) },
            )
        }
    }
}
