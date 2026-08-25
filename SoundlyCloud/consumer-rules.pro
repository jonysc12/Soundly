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

# Soundly Cloud Internal Models
-keep class com.soundly.cloud.** { *; }

# Missing classes referenced by dependencies in SoundlyCloud
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.imageio.**
-dontwarn javax.script.**
-dontwarn javax.swing.**
-dontwarn jdk.dynalink.**
-dontwarn org.tukaani.xz.**
