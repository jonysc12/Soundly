plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val roomTempDir = layout.projectDirectory.dir("tmp/room-sqlite").asFile

android {
    namespace = "com.soundly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.soundly"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments(
                    mapOf(
                        "room.schemaLocation" to "$projectDir/tmp/room-schemas",
                        "room.verifySchemas" to "false",
                        "room.incremental" to "true",
                        "room.expandProjection" to "true",
                        "room.sqlite.native.lib" to roomTempDir.absolutePath
                    )
                )
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("2.0.0")
            because("Align Kotlin stdlib to 2.0.0 for Room metadata compatibility")
        }
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask::class.java).configureEach {
    doFirst { roomTempDir.mkdirs() }
    kaptProcessJvmArgs.addAll(
        listOf("-Dorg.sqlite.tmpdir=${roomTempDir.absolutePath}")
    )
}

dependencies {
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-unit")
    implementation(libs.androidx.compose.material.core)
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material3:material3:1.5.0-alpha16")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("dev.chrisbanes.haze:haze:1.6.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kaptDebug("androidx.room:room-compiler:2.6.1")
    kaptRelease("androidx.room:room-compiler:2.6.1")
    implementation("ir.mahozad.multiplatform:wavy-slider-android:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
