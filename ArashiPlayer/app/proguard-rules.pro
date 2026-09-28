# ---- Media3 / ExoPlayer ----
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.arashiplayer.data.net.** {
    *** Companion;
}
-keepclasseswithmembers class com.arashiplayer.data.net.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Room ----
-keep class com.arashiplayer.data.local.** { *; }

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Coil ----
-dontwarn coil.**
