# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# NewPipeExtractor
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }
-keep class * extends org.schabi.newpipe.extractor.StreamingService { *; }
-keep class * extends org.schabi.newpipe.extractor.Extractor { *; }
-keep class com.grack.nanojson.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn com.grack.nanojson.**

# youtubedl-android
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Python and native assets
-keep class com.srplab.** { *; }
-dontwarn com.srplab.**

# Apache Commons Compress (Used by YoutubeDL-Android)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# BlurView
-keep class eightbitlab.com.blurview.** { *; }

# Jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Soundly Cloud Models
-keep class com.soundly.cloud.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.soundly.data.local.** { *; }
-keep class com.soundly.data.local.**_Impl { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.android.internal.** { *; }
-keep class com.soundly.DaggerSoundlyApp_HiltComponents_SingletonC** { *; }
-keep class * {
    @dagger.hilt.android.EntryPoint *;
}

# Missing classes detected by R8
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.imageio.**
-dontwarn javax.script.**
-dontwarn javax.swing.**
-dontwarn jdk.dynalink.**
-dontwarn kotlin.uuid.**
-dontwarn org.tukaani.xz.**

# Jsoup
-keep class org.jsoup.** { *; }

# Kotlin Reflect (sometimes needed for native lib loading)
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
