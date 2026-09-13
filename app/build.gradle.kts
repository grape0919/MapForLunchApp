import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(name: String): String = localProps.getProperty(name) ?: ""

android {
    namespace = "info.hkdevstudio.gom"
    compileSdk = 35

    defaultConfig {
        applicationId = "info.hkdevstudio.gom"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "2.0.0"

        buildConfigField("String", "KAKAO_REST_API_KEY", "\"${secret("KAKAO_REST_API_KEY")}\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${secret("KAKAO_NATIVE_APP_KEY")}\"")
        // 기본값은 구글 공식 테스트 광고 ID — 출시 시 local.properties 에 실제 값 설정
        buildConfigField(
            "String", "ADMOB_BANNER_ID",
            "\"${secret("ADMOB_BANNER_ID").ifBlank { "ca-app-pub-3940256099942544/6300978111" }}\""
        )
        manifestPlaceholders["ADMOB_APP_ID"] =
            secret("ADMOB_APP_ID").ifBlank { "ca-app-pub-3940256099942544~3347511713" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)
    implementation(libs.kakao.map)
    implementation(libs.coil.compose)
}
