import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = System.getenv("MAPS_API_KEY")
    ?: localProperties.getProperty("MAPS_API_KEY")
    ?: localProperties.getProperty("maps.api.key")
    ?: ""

val apiBaseUrl: String = (System.getenv("API_BASE_URL")
    ?: localProperties.getProperty("API_BASE_URL")
    ?: localProperties.getProperty("api.base.url")
    ?: "http://10.0.2.2:8080").trimEnd('/')

android {
    namespace = "com.example.easysport"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.easysport"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        manifestPlaceholders["CLEARTEXT_ALLOWED"] = "true"
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("Boolean", "HAS_MAPS_API_KEY", (mapsApiKey.isNotBlank()).toString())
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            manifestPlaceholders["CLEARTEXT_ALLOWED"] = "true"
        }
        release {
            manifestPlaceholders["CLEARTEXT_ALLOWED"] = "false"
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.browser:browser:1.8.0")

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}

tasks.register("setupAdbReverse") {
    doLast {
        try {
            val sdkDir = localProperties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
            val adbExe = if (!sdkDir.isNullOrBlank()) {
                val candidate = file("$sdkDir/platform-tools/adb.exe")
                if (candidate.exists()) candidate.absolutePath else "adb"
            } else {
                "adb"
            }
            val process = ProcessBuilder(adbExe, "reverse", "tcp:8080", "tcp:8080").start()
            process.waitFor()
        } catch (_: Exception) {}
    }
}

tasks.matching { it.name.startsWith("install") || it.name == "assembleDebug" }.configureEach {
    dependsOn("setupAdbReverse")
}
