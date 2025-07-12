plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
}

android {
    namespace = "com.rto1p8.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rto1p8.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
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
        dataBinding = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.play.services.auth.api.phone)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("androidx.work:work-runtime-ktx:2.10.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation ("com.google.code.gson:gson:2.11.0")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("com.airbnb.android:lottie:6.6.2")

}