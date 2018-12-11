# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-keepattributes Signature

-keep,includedescriptorclasses class xyz.mattishub.campusDual.**  { public *; }

-keep class kotlinx.coroutines.** { public *; }
-keep class org.joda.time.tz.data.ZoneInfoMap { public *; }
