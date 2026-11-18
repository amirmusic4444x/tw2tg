plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.amir.xlite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amir.xlite"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val tok = System.getenv("APP_TOKEN") ?: ""
        buildConfigField("String", "GH_TOKEN", "\"$tok\"")
        buildConfigField("String", "GH_OWNER", "\"amirmusic4444x\"")
        buildConfigField("String", "GH_REPO", "\"tw2tg\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
}
