plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ryvk.drifthome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ryvk.drifthome"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Add the dependency for the Firebase Authentication library
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Add the dependency for the Google Maps API
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Add the dependency for the Google Maps Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Add the dependency for the Firebase Firestore
    implementation("com.google.firebase:firebase-firestore")

    //Add the dependancy for the Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")

    //Add the dependancy for the Firebase Storage
    implementation("com.google.firebase:firebase-storage")

    // Add OKHttp client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //add GSON
    implementation ("com.google.code.gson:gson:2.11.0")

    //add glide for GIF manipulation
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    //add library for uploading cropped profile images
    implementation("com.github.yalantis:ucrop:2.2.6")

    //add PayHere SDK
    implementation ("com.github.PayHereDevs:payhere-android-sdk:v3.0.17")
    implementation ("androidx.appcompat:appcompat:1.6.0")

}