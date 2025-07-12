plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
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

    signingConfigs {
        create("release") {
            // For testing - replace with your actual keystore
            storeFile = file("../keystore/release.keystore")
            storePassword = "test123"
            keyAlias = "test_alias"
            keyPassword = "test123"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
}

// Custom task to encrypt manifest before building
tasks.register("encryptManifest") {
    group = "build"
    description = "Encrypts the real AndroidManifest.xml"
    
    doLast {
        val realManifestFile = file("src/main/assets/real_manifest.xml")
        val encryptedFile = file("src/main/assets/encrypted_manifest.dat")
        
        // Ensure assets directory exists
        encryptedFile.parentFile.mkdirs()
        
        if (realManifestFile.exists()) {
            // Read and encrypt the manifest
            val manifestContent = realManifestFile.readText()
            val encryptedData = encryptManifestContent(manifestContent)
            encryptedFile.writeBytes(encryptedData)
            
            println("✅ Manifest encrypted successfully: ${encryptedFile.absolutePath}")
        } else {
            println("⚠️ Real manifest not found at: ${realManifestFile.absolutePath}")
            // Create a template real manifest
            realManifestFile.writeText("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.rto1p8.app">
    
    <!-- Your real manifest content goes here -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:name="com.rto1p8.app.security.SecureApplication"
        android:label="@string/app_name">
        
        <!-- Your real activities, services, receivers go here -->
        
    </application>
</manifest>""")
            println("📝 Created template real manifest. Please edit it with your actual configuration.")
        }
    }
}

// Hook encryption into the build process
tasks.whenTaskAdded {
    if (name.contains("merge") && name.contains("Assets")) {
        dependsOn("encryptManifest")
    }
}

// Encryption function
fun encryptManifestContent(content: String): ByteArray {
    val key = "MySecretKey123456789012345678901" // 32 bytes for AES-256
    val iv = "1234567890123456" // 16 bytes for AES
    
    val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "AES")
    val ivSpec = javax.crypto.spec.IvParameterSpec(iv.toByteArray())
    
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec)
    return cipher.doFinal(content.toByteArray(Charsets.UTF_8))
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
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.airbnb.android:lottie:6.6.2")
}