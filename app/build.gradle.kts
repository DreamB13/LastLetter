import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.gms.google-services")

}

android {
    signingConfigs {
        create("release") {
            storeFile = file("C:\\Users\\seeju\\lastletter.jks")
            storePassword = "Tlwns*8!@@"
            keyAlias = "key0"
            keyPassword = "Tlwns*8!@@"
        }
    }
    namespace = "com.ksj.lastletter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ksj.lastletter"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // properties가 없을 경우를 대비해 기본값을 빈 문자열로 처리
        buildConfigField("String", "KAKAO_API_KEY", "\"${properties["KAKAO_API_KEY"] ?: ""}\"")
        resValue("string", "KAKAO_API_KEY", "\"${properties["KAKAO_API_KEY"] ?: ""}\"")
        buildConfigField("String", "KAKAO_REDIRECT_URI", "\"${properties["KAKAO_REDIRECT_URI"] ?: ""}\"")
        resValue("string", "KAKAO_REDIRECT_URI", "\"${properties["KAKAO_REDIRECT_URI"] ?: ""}\"")
        buildConfigField("String", "client_id", "\"${properties["client_id"] ?: ""}\"")
        resValue("string", "client_id", "\"${properties["client_id"] ?: ""}\"")

        // Google Speech-to-Text API 키 추가
        // (1) local.properties에서 GOOGLE_API_KEY 읽어오기
        val localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            val properties = Properties()
            properties.load(localProperties.inputStream())
            val googleApiKey: String = properties.getProperty("GOOGLE_API_KEY") ?: ""

            // (2) BuildConfig에 GOOGLE_API_KEY 필드 추가
            buildConfigField("String", "GOOGLE_API_KEY", "\"$googleApiKey\"")
        } else {
            // local.properties가 없을 경우 대비
            buildConfigField("String", "GOOGLE_API_KEY", "\"\"")
        }
    }
    buildFeatures {
        buildConfig = true
        // Compose 사용 시 활성화 (필요한 경우)
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // kotlinOptions는 별도의 블록으로 선언
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // 패키징 옵션 추가 - 의존성 충돌 방지
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    val nav_version = "2.8.8"

    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Google 로그인 관련 의존성
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase 플랫폼 BoM 사용
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")

    // REST API 기반 STT 사용을 위한 OkHttp 추가
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // 코루틴 라이브러리
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // 카카오 SDK (필요한 모듈만 선택하여 적용)
    implementation("com.kakao.sdk:v2-all:2.20.6")

    implementation("androidx.datastore:datastore-preferences:1.1.3")
    implementation("androidx.datastore:datastore-preferences-core:1.1.3")

    //FastAPI전송 - retrofit
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    //애니메이션 처리
    implementation ("com.github.bumptech.glide:glide:4.13.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.13.2")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
}