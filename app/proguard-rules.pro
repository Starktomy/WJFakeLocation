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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Fix R8 Missing Classes errors
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.script.**

# Xposed
-keep class com.steadywj.wjfakelocation.xposed.** { *; }
-keep class de.robv.android.xposed.** { *; }

# Baidu Map
-keep class com.baidu.** { *; }
-keep class vi.com.** { *; }
-dontwarn com.baidu.**

# AMap (Gaode Map)
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.a.a.** { *; }
-dontwarn com.amap.**
-dontwarn com.autonavi.**
-dontwarn com.a.a.**

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
