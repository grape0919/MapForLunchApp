package kr.ai.redbridgedev.bobmap

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
import androidx.navigation.navDeepLink
import kr.ai.redbridgedev.bobmap.domain.Place
import kr.ai.redbridgedev.bobmap.util.ShareUtils
import kr.ai.redbridgedev.bobmap.ui.MainViewModel
import kr.ai.redbridgedev.bobmap.ui.screen.KakaoPlaceWebScreen
import kr.ai.redbridgedev.bobmap.ui.screen.MapScreen
import kr.ai.redbridgedev.bobmap.ui.screen.OnboardingScreen
import kr.ai.redbridgedev.bobmap.ui.screen.PlaceDetailScreen
import kr.ai.redbridgedev.bobmap.ui.screen.RecordsScreen
import kr.ai.redbridgedev.bobmap.ui.screen.RouletteScreen
import kr.ai.redbridgedev.bobmap.ui.theme.Cream
import kr.ai.redbridgedev.bobmap.ui.theme.GomTheme
import kr.ai.redbridgedev.bobmap.util.LocationProvider
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
    const val PLACE = "place/{placeId}?visitId={visitId}&n={n}&c={c}&a={a}&lat={lat}&lng={lng}&id={id}"
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
            kr.ai.redbridgedev.bobmap.ui.screen.Wordmark()
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
                // 쿼리형 딥링크(/place/?id=..)에는 경로 인자가 없으므로 선택 인자로 둔다.
                // 필수로 두면 Navigation이 그래프 생성 시 "required arguments are missing"으로 앱을 종료시킨다.
                navArgument("placeId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("visitId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("n") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("c") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("a") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
            // 공유 링크: https://bobmap-link.redbridgedev.ai.kr/place/{id}?n=..  /  bobmap://place/{id}?n=..
            deepLinks = listOf(
                // 공유 링크(현행): /place/?id=..&n=..
                navDeepLink { uriPattern = "https://${ShareUtils.LINK_HOST}/place/?id={id}&n={n}&c={c}&a={a}&lat={lat}&lng={lng}" },
                navDeepLink { uriPattern = "https://${ShareUtils.LINK_HOST}/place/?id={id}" },
                // 경로형(호환): /place/{id}
                navDeepLink { uriPattern = "https://${ShareUtils.LINK_HOST}/place/{placeId}?n={n}&c={c}&a={a}&lat={lat}&lng={lng}" },
                navDeepLink { uriPattern = "https://${ShareUtils.LINK_HOST}/place/{placeId}" },
                navDeepLink { uriPattern = "bobmap://place/?id={id}&n={n}&c={c}&a={a}&lat={lat}&lng={lng}" },
                navDeepLink { uriPattern = "bobmap://place/{placeId}?n={n}&c={c}&a={a}&lat={lat}&lng={lng}" },
                navDeepLink { uriPattern = "bobmap://place/{placeId}" },
            ),
        ) { entry ->
            val args = entry.arguments
            // 쿼리형 링크(id=)가 오면 경로 세그먼트가 비어 있으므로 id 인자를 우선한다
            val placeId = args?.getString("id")?.takeIf { it.isNotBlank() } ?: args?.getString("placeId").orEmpty()
            val visitId = args?.getLong("visitId")?.takeIf { it > 0 }
            val linkedName = args?.getString("n")
            if (!linkedName.isNullOrBlank()) {
                viewModel.rememberExternalPlace(
                    Place(
                        id = placeId, name = linkedName,
                        category = args.getString("c").orEmpty(), fullCategory = args.getString("c").orEmpty(),
                        phone = "", address = args.getString("a").orEmpty(),
                        lat = args.getString("lat")?.toDoubleOrNull() ?: 0.0,
                        lng = args.getString("lng")?.toDoubleOrNull() ?: 0.0,
                        placeUrl = "https://place.map.kakao.com/$placeId", distanceM = null,
                    )
                )
            }
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
