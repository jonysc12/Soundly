plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
}

val roomTempDir = layout.projectDirectory.dir("tmp/room-sqlite").asFile

android {
    namespace = "com.soundly"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soundly"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments(
                    mapOf(
                        "room.schemaLocation" to layout.projectDirectory.dir("tmp/room-schemas").asFile.absolutePath,
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
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes += "**/lib*debug.so"
        }
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1,*.SF,*.RSA,*.DSA}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

val createRoomTempDir = tasks.register("createRoomTempDir") {
    val dir = roomTempDir
    outputs.dir(dir)
    doLast {
        dir.mkdirs()
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask::class.java).configureEach {
    dependsOn(createRoomTempDir)
    kaptProcessJvmArgs.add("-Dorg.sqlite.tmpdir=${roomTempDir.absolutePath}")
}

dependencies {
    implementation(project(":SoundlyCloud"))
    implementation(libs.androidx.animation)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.unit)
    
    // Removed BOM to avoid version conflicts with M3 1.4.0
    
    implementation(libs.androidx.material3)
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.github.Dimezis:BlurView:version-2.0.3")
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    "baselineProfile"(project(":baselineprofile"))
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("ir.mahozad.multiplatform:wavy-slider-android:1.0.0")
    implementation(libs.reorderable)
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast.framework)
    implementation(libs.media3.cast)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
