package kr.ai.redbridgedev.bobmap

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.kakao.vectormap.KakaoMapSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@GomApplication)
        }
    }
}
