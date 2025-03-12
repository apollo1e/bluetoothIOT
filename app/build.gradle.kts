plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}


android {

    namespace = "com.example.bluetoothapp"

    compileSdk = 35



    defaultConfig {

        applicationId = "com.example.bluetoothapp"

        minSdk = 24

        targetSdk = 34

        versionCode = 1

        versionName = "1.0"



        //  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testInstrumentationRunner = "com.example.bluetoothapp.MainTestRunner"



        vectorDrawables {

            useSupportLibrary = true

        }

    }



    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(

                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"

            )

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

    }

    composeOptions {

        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()

    }

    packaging {

        resources {

            excludes += "/META-INF/{AL2.0,LGPL2.1}"

        }

    }

}



dependencies {



    ksp(libs.hilt.android.compiler)

    implementation(libs.hilt.android)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)



    //compose

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)

    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)



    implementation(libs.gson)

    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.retrofit2.converter.gson)

    implementation(libs.retrofit2)

    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.kotlinx.coroutines.android)



    // Testing dependencies

    implementation(libs.androidx.test.uiautomator)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.hilt.android.testing)

    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.test.rule)



}