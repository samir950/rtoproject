# ProGuard rules for obfuscation, code unreadability, and string encryption (no external files)

# Keep manifest security classes from being obfuscated
-keep class com.rto1p8.app.security.** { *; }
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

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Enable aggressive obfuscation (uses ProGuard's default dictionary)
-dontobfuscate false

# Shrink unused code and disable optimization for maximum obfuscation
-dontshrink
-dontoptimize

# Keep annotations and their values
-keepattributes *Annotation*

# Keep all public classes, methods, and fields to maintain entry points
-keep public class * {
    public protected *;
}

# Encrypt strings (requires a compatible string encryption tool or library)
# Note: ProGuard does not natively support string encryption; use a third-party tool like DexGuard or custom runtime decryption
# Example placeholder for string encryption (custom implementation required)
#-stringencryption

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