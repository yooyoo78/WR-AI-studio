# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information for crash stack traces while keeping source filenames clean
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin reflection / metadata needed by Moshi KotlinJsonAdapterFactory and Coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# Moshi Codegen & Reflective JSON models (Seed Pack, OTA Manifest, AI JSON schemas)
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(...);
}
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }

# Room Entities, DAOs, and Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.data.local.** { *; }

# Keep string resources accessed indirectly or by SDKs
-keepclassmembers class com.example.R$* {
    public static <fields>;
}

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Firebase App Check / reCAPTCHA / AI Logic
-dontwarn com.google.firebase.**
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.google.firebase.ai.** { *; }
-keep class com.google.firebase.appcheck.** { *; }
