import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.anxinkan.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.anxinkan.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "WEATHER_WORKER_BASE_URL", "\"\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(providers.environmentVariable("ANXINKAN_RELEASE_KEYSTORE").orNull ?: "")
            storePassword = providers.environmentVariable("ANXINKAN_RELEASE_STORE_PASSWORD").orNull
            keyAlias = "anxinkan"
            keyPassword = providers.environmentVariable("ANXINKAN_RELEASE_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            val workerUrl = providers.environmentVariable("WEATHER_WORKER_BASE_URL").orElse("").get()
            buildConfigField("String", "WEATHER_WORKER_BASE_URL", "\"$workerUrl\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        doFirst {
            val workerUrl = providers.environmentVariable("WEATHER_WORKER_BASE_URL").orElse("").get()
            if (!workerUrl.startsWith("https://")) {
                throw GradleException("Release 构建必须设置 WEATHER_WORKER_BASE_URL 为已部署的 https Worker 地址")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
