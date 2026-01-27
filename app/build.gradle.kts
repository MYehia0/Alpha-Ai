import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
}

// Load API keys from properties file
val apiProperties = Properties().apply {
    val apiPropertiesFile = rootProject.file("api_keys.properties")
    if (apiPropertiesFile.exists()) {
        load(FileInputStream(apiPropertiesFile))
    } else {
        // Create default empty properties if file doesn't exist
        setProperty("GEC_API_KEY", "")
        setProperty("GECAR_API_KEY", "")
        setProperty("LDF_API_KEY", "")
        setProperty("OCR_API_KEY", "")
        setProperty("SST_API_KEY", "")
        setProperty("TTS_API_KEY", "")
        setProperty("MT_API_KEY", "")
        setProperty("TG_API_KEY", "")
        setProperty("IG_API_KEY", "")
        setProperty("SUM_API_KEY", "")
        setProperty("SUMMAR_API_KEY", "")
    }
}

android {
    namespace = "com.example.alpha_ai"
    compileSdk = 34
    
    // Enable view binding
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.alpha_ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        // Add API keys to BuildConfig
        buildConfigField("String", "GEC_API_KEY", "\"${apiProperties.getProperty("GEC_API_KEY", "")}\"")
        buildConfigField("String", "GECAR_API_KEY", "\"${apiProperties.getProperty("GECAR_API_KEY", "")}\"")
        buildConfigField("String", "LDF_API_KEY", "\"${apiProperties.getProperty("LDF_API_KEY", "")}\"")
        buildConfigField("String", "OCR_API_KEY", "\"${apiProperties.getProperty("OCR_API_KEY", "")}\"")
        buildConfigField("String", "SST_API_KEY", "\"${apiProperties.getProperty("SST_API_KEY", "")}\"")
        buildConfigField("String", "TTS_API_KEY", "\"${apiProperties.getProperty("TTS_API_KEY", "")}\"")
        buildConfigField("String", "MT_API_KEY", "\"${apiProperties.getProperty("MT_API_KEY", "")}\"")
        buildConfigField("String", "TG_API_KEY", "\"${apiProperties.getProperty("TG_API_KEY", "")}\"")
        buildConfigField("String", "IG_API_KEY", "\"${apiProperties.getProperty("IG_API_KEY", "")}\"")
        buildConfigField("String", "SUM_API_KEY", "\"${apiProperties.getProperty("SUM_API_KEY", "")}\"")
        buildConfigField("String", "SUMMAR_API_KEY", "\"${apiProperties.getProperty("SUMMAR_API_KEY", "")}\"")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Build features are now defined at the top of the android block
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("me.zhanghai.android.materialprogressbar:library:1.6.1")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // datastore
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Retrofit & Networking
    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    
    // OkHttp
    val okhttpVersion = "4.11.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    // Coroutines
    val coroutinesVersion = "1.7.3"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Lifecycle
    val lifecycleVersion = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Apache POI
    val poiVersion = "5.2.3"
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("org.apache.poi:poi-scratchpad:$poiVersion")

    // UI Components
    implementation(libs.swipe.layout)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Glide for image loading (if needed)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    
    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")
}

kapt {
    correctErrorTypes = true
}