plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.suibari.skyputter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.suibari.skyputter"
        minSdk = 26
        targetSdk = 35
        versionCode = 4 // リリース時はここをインクリメント
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    // Android標準のLogなどがモックされていない場合に、スルーする
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 基本的なAndroid依存関係
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // テスト関連
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.7")
    // UIテスト
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")

    // WorkManager（バックグラウンド処理用）
    implementation(libs.androidx.work.runtime.ktx)

    // Bluesky API
    implementation("work.socialhub.kbsky:core:0.3.0")
    implementation("work.socialhub.kbsky:auth:0.3.0")
    implementation("work.socialhub.kbsky:stream:0.3.0")

    // データ永続化
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // UI関連
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.foundation:foundation:1.6.1")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.compose.material3:material3:1.3.2") // PullRefresh対応バージョン以降
    implementation("androidx.compose.material:material-icons-extended:x.x.x") // Icons

    // 下書き機能
    implementation("com.google.code.gson:gson:2.10.1")

    // 動画再生
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")
    implementation("androidx.media3:media3-common:1.3.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1") // HLS(.m3u8)再生用

    // === 1分間隔通知のための追加依存関係 ===
    // フォアグラウンドサービス用
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // コルーチン（既存でも念のため）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel（既存でも念のため）
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // AlarmManager用（Android標準APIなので追加依存関係不要だが、念のためKTX）
    implementation("androidx.core:core-ktx:1.12.0")

    // 権限処理用（必要に応じて）
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}