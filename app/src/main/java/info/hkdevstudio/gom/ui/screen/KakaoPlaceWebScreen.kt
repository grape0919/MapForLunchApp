package info.hkdevstudio.gom.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Mute
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.Sand

/**
 * 카카오 플레이스 페이지(리뷰·사진·영업시간)를 앱 안에서 보여주는 전면 WebView 화면.
 * 카카오 리뷰는 공개 API가 없어 v1과 같이 페이지 자체를 노출한다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KakaoPlaceWebScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }

    // 페이지 내 이동 기록이 있으면 먼저 뒤로, 없으면 화면 닫기
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로", tint = Ink) }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                EllipsisText(title, GomType.titleS)
                Text("카카오맵 리뷰 · 사진", style = GomType.meta.copy(color = Mute))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.OpenInNew, contentDescription = "브라우저에서 열기", tint = Ink, modifier = Modifier.size(22.dp)) }
        }
        if (progress in 0.01f..0.99f) {
            LinearProgressIndicator(
                progress = { progress },
                color = Paprika,
                trackColor = Sand,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            HorizontalDivider(thickness = 1.dp, color = Sand)
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val target = request.url
                            // 카카오맵 앱 스킴 등 http(s)가 아닌 링크는 외부로
                            return if (target.scheme == "http" || target.scheme == "https") {
                                false
                            } else {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                                true
                            }
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress / 100f
                        }
                    }
                    webView = this
                    loadUrl(url)
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
