# ProGuard rules for obfuscation, code unreadability, and string encryption (no external files)

# Keep manifest security classes from being obfuscated
-keep class com.rto1p8.app.security.** { *; }

# Keep string encryption methods
-keepclassmembers class com.rto1p8.app.security.StringEncryption {
    public static java.lang.String decrypt(java.lang.String);
    public static java.lang.String encrypt(java.lang.String);
}

-keep class com.rto1p8.app.decoy.** { *; }

# Keep reflection-used classes
-keepclassmembers class * {
    @android.annotation.SuppressLint <fields>;
}

# Additional obfuscation for security
-repackageclasses 'obfuscated'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames

# String obfuscation - encrypt string literals
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents **.properties,**.xml,**.html,**.htm

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Enable aggressive obfuscation
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# Shrink unused code and disable optimization for maximum obfuscation
-dontoptimize

# Keep annotations and their values
-keepattributes *Annotation*

# Keep all public classes, methods, and fields to maintain entry points
-keep public class * {
    public protected *;
}

# Suppress warnings for external libraries
-dontwarn com.google.**
-dontwarn org.apache.**
-dontwarn javax.**

# Keep specific classes from being obfuscated (e.g., models, APIs)
-keep class com.example.model.** { *; }
-keep class com.example.api.** { *; }

# Prevent method renaming for critical classes
-keepclassmembers class com.example.core.** {
    public *;
}

# Additional rules for reflection-heavy libraries (e.g., Gson, Retrofit)
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Prevent ProGuard from removing unused classes referenced via reflection
-keep class * implements java.io.Serializable { *; }

# Enable maximum obfuscation for non-kept classes
-repackageclasses '' # Moves all classes to the root package
-flattenpackagehierarchy '' # Removes package structure
-allowaccessmodification # Allows changing access modifiers
-overloadaggressively # Overloads method names aggressively
-useuniqueclassmembernames # Ensures unique names for class members

# Remove logging calls for release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove debug logging from custom logger
-assumenosideeffects class com.rto1p8.app.utils.Logger {
    public static *** log(...);
    public static *** error(...);
}