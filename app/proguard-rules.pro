# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
    static <1>$$serializer INSTANCE;
}

# Keep class names of patching steps since they're used via reflection
-keepnames class com.aliucord.manager.patcher.steps.**

# Repackage classes into the top-level.
-repackageclasses

# Amount of optimization iterations, taken from an SO post
-optimizationpasses 5
-mergeinterfacesaggressively

# Broaden access modifiers to increase results during optimization
-allowaccessmodification

# Suppress missing class warnings
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Keeps all class and field names for debugging
-keepnames class ** { *; }

# Preserve the line number information for debugging stack traces.
-keepattributes LineNumberTable

# Fix LSPatch breaking
# ref: https://github.com/LSPosed/LSPatch/blob/bbe8d93fb9230f7b04babaf1c4a11642110f55a6/manager/proguard-rules.pro#L12-L18
-keep class com.beust.jcommander.** { *; }
-keep class org.lsposed.lspatch.Patcher$Options { *; }
-keep class org.lsposed.lspatch.share.LSPConfig { *; }
-keep class org.lsposed.lspatch.share.PatchConfig { *; }
-keepclassmembers class org.lsposed.patch.LSPatch {
    private <fields>;
}
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
