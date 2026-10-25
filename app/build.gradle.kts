plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "bo.biogasgama"
    compileSdk = 35

    defaultConfig {
        applicationId = "bo.biogasgama"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Clave de Google Maps: secreto MAPS_API_KEY en GitHub Actions o propiedad local de Gradle.
        val claveMapas = System.getenv("MAPS_API_KEY") ?: (project.findProperty("MAPS_API_KEY") as String?) ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = claveMapas
        buildConfigField("boolean", "TIENE_CLAVE_MAPAS", (claveMapas.isNotBlank()).toString())
    }

    // Firma fija: así cada APK nuevo se instala encima del anterior.
    signingConfigs {
        create("fija") {
            storeFile = file("firma.jks")
            storePassword = "biogasgama"
            keyAlias = "biogasgama"
            keyPassword = "biogasgama"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fija")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Ubicación GNSS fusionada
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Google Maps para Compose
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    testImplementation("junit:junit:4.13.2")
}
