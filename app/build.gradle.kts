import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

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

        // Opcional: en local.properties añade EXCHANGE_RATE_API_KEY=tu_clave para exchangerate-api.com
        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }
        val exchangeKey = localProperties.getProperty("EXCHANGE_RATE_API_KEY", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "EXCHANGE_RATE_API_KEY", "\"$exchangeKey\"")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//libs
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit.main)
    implementation(libs.retrofit.gson)
}