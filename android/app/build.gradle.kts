plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

// URL de la API para las builds release. Tiene que ser HTTPS y no se commitea:
//   ./gradlew assembleRelease -Pmatchbar.releaseApiUrl=https://api.tudominio.com/
// (o define matchbar.releaseApiUrl en ~/.gradle/gradle.properties).
val releaseApiUrl: String = providers.gradleProperty("matchbar.releaseApiUrl").orNull ?: ""

/** Valor de ~/.gradle/gradle.properties (-P) o, si no, de una variable de entorno (CI). */
fun secret(property: String, env: String): String? =
    providers.gradleProperty(property).orElse(providers.environmentVariable(env)).orNull

// Firma de la release (ver README). Si no se configura, assembleRelease genera
// un APK sin firmar: sirve para comprobar el build, no para publicar.
val keystorePath = secret("matchbar.keystore.path", "MATCHBAR_KEYSTORE_PATH")

android {
    namespace = "com.matchbar.app"
    // compileSdk solo fija contra qué APIs se compila (Compose 1.12 y lifecycle
    // 2.11 exigen 37); el comportamiento de Android lo decide targetSdk.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.matchbar.app"
        minSdk = 26
        // Google Play exige API 36 para apps nuevas y actualizaciones desde el 31/08/2026.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = secret("matchbar.keystore.password", "MATCHBAR_KEYSTORE_PASSWORD")
                keyAlias = secret("matchbar.key.alias", "MATCHBAR_KEY_ALIAS")
                keyPassword = secret("matchbar.key.password", "MATCHBAR_KEY_PASSWORD")
            }
        }
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
            // R8: reduce y ofusca el código y elimina recursos no usados.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        // Con Kotlin integrado en AGP 9, el jvmTarget de Kotlin sigue a este valor.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
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
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0") // LocalLifecycleOwner

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Networking: Retrofit + OkHttp + Kotlinx Serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // DataStore (almacenamiento del JWT)
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Coil (imágenes)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Mapas: OpenStreetMap vía osmdroid (gratis, sin API key)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Ubicación del dispositivo (FusedLocationProvider; no requiere API key de mapas)
    implementation("com.google.android.gms:play-services-location:21.4.0")
    // play-services arrastra Fragment 1.1.0, con el que registerForActivityResult
    // (permiso de ubicación) puede fallar; forzamos una versión actual (>= 1.3.0).
    implementation("androidx.fragment:fragment:1.9.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
