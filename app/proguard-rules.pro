# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimization flags for smaller APK
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove debug information and line numbers for release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep accessibility service configuration
-keep class com.krishdev.searchassist.SimpleAccessibilityService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Keep Compose-related classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Material3 classes that might be accessed via reflection
-keep class androidx.compose.material3.** { *; }

# Keep fragment classes
-keep class * extends androidx.fragment.app.Fragment { *; }
-keep class com.krishdev.searchassist.*Fragment { *; }

# Keep main application components
-keep class com.krishdev.searchassist.MainActivity { *; }
-keep class com.krishdev.searchassist.BootReceiver { *; }

# Remove unused resources and code
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile