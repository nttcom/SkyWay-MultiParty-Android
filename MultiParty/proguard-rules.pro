# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/adt-bundle/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontskipnonpubliclibraryclasses
-verbose

# Disable optimizations. Some of them are broken on Android
-dontoptimize
#-optimizations !code/simplification/cast,!field/*,!class/merging/*
#-optimizationpasses 10

# Preverification is required by J2ME and Java 7, but not by Android.
# Dex apparently doesn't like it either.
-dontpreverify

# Keep method parameter names to assist IDE autocompletion.
-keepparameternames

# A bunch of things useful for users of a library.
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,SourceFile,LineNumberTable

# Keep public classes and members.
-keep public class * {
    public *;
}

# Keep native methods.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep parcelables and their "Creator" field.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}
