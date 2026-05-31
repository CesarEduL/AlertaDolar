import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun propertyOrEnv(key: String): String =
    localProperties.getProperty(key)?.trim().orEmpty().ifBlank {
        System.getenv(key)?.trim().orEmpty()
    }

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.waytolearn.alertadolar"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.waytolearn.alertadolar"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties o variable de entorno EXCHANGE_RATE_API_KEY (p. ej. en GitHub Actions)
        val exchangeKey = propertyOrEnv("EXCHANGE_RATE_API_KEY").escapeForBuildConfig()
        buildConfigField("String", "EXCHANGE_RATE_API_KEY", "\"$exchangeKey\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = propertyOrEnv("RELEASE_KEYSTORE_PATH")
            if (keystorePath.isNotBlank()) {
                storeFile = file(keystorePath)
                storePassword = propertyOrEnv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = propertyOrEnv("RELEASE_KEY_ALIAS")
                keyPassword = propertyOrEnv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePath = propertyOrEnv("RELEASE_KEYSTORE_PATH")
            if (keystorePath.isNotBlank() && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//libs
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit.main)
    implementation(libs.retrofit.gson)
}
