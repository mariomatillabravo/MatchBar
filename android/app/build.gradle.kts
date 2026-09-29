plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")   // ← AÑADE ESTA
}

// URL de la API para las builds release. Tiene que ser HTTPS y no se commitea:
//   ./gradlew assembleRelease -Pmatchbar.releaseApiUrl=https://api.tudominio.com/
// (o define matchbar.releaseApiUrl en ~/.gradle/gradle.properties).
val releaseApiUrl: String = providers.gradleProperty("matchbar.releaseApiUrl").orNull ?: ""

android {
    namespace = "com.matchbar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.matchbar.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // 10.0.2.2 = localhost del PC desde el emulador Android. Para un móvil
            // físico, pon aquí la IP de tu PC y añádela también a
            // src/debug/res/xml/network_security_config.xml.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    //composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Impide generar una release que envíe contraseñas y tokens sin cifrar.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(releaseApiUrl.startsWith("https://")) {
            "La build release necesita una URL HTTPS: -Pmatchbar.releaseApiUrl=https://api.tudominio.com/"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Networking: Retrofit + OkHttp + Kotlinx Serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore (almacenamiento del JWT)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (imágenes)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Mapas: OpenStreetMap vía osmdroid (gratis, sin API key)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Ubicación del dispositivo (FusedLocationProvider; no requiere API key de mapas)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
