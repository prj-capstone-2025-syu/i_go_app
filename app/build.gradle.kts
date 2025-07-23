plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.igo_ai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.igo_ai"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ""
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}


androidComponents {
    onVariants { variant ->
        val buildTypeName = variant.buildType

        var finalVersionName = project.android.defaultConfig.versionName
        if (variant.buildType == "debug" && project.android.buildTypes.getByName("debug").versionNameSuffix != null) {
            finalVersionName += project.android.buildTypes.getByName("debug").versionNameSuffix
        }



        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(
                "IGO_AI_v${finalVersionName}_${buildTypeName}.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("androidx.work:work-runtime:2.9.0")
}

