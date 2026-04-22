# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash report stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- App data classes (Gson serialization via Retrofit) ---
-keep class sk.spacirkovnik.model.** { *; }
-keep class sk.spacirkovnik.data.GameIndexResponse { *; }

# --- Gson @SerializedName field annotations ---
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit method annotations on interfaces ---
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
