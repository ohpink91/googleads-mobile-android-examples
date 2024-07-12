plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.example.jetpackcomposedemo"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.google.android.gms.example.jetpackcomposedemo"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    vectorDrawables { useSupportLibrary = true }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions { jvmTarget = "1.8" }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
  implementation("androidx.activity:activity")
  implementation("androidx.activity:activity-ktx")
  implementation("androidx.activity:activity-compose:1.9.0")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
  implementation(platform("androidx.compose:compose-bom:2024.06.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.navigation:navigation-compose:2.7.7")
  implementation("com.google.android.gms:play-services-ads:23.2.0")
  implementation("com.google.android.ump:user-messaging-platform:2.2.0")
  implementation("com.google.android.gms:play-services-ads-lite:23.2.0")
  implementation(project(":compose-support"))
  debugImplementation("androidx.compose.ui:ui-tooling")
}
