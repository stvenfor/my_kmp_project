# Release R8 rules for :composeApp (KMP + Compose Multiplatform, Android target).
#
# Compose / AndroidX / OkHttp / Coil / Ktor ship their own consumer rules; only the
# reflection-based paths that the consumer rules do not cover are pinned here.

# ---------------------------------------------------------------------------
# kotlinx.serialization — generated `Companion.serializer()` + `$$serializer` classes
# are looked up reflectively by the runtime.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.my_kmp_project.**$$serializer { *; }
-keepclassmembers class com.example.my_kmp_project.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.my_kmp_project.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Ktor — engine/plugin discovery goes through ServiceLoader and reflection on
# platform engines; the 3.x artifacts keep their own rules but the client module
# is still selected by name on Android.
# ---------------------------------------------------------------------------
-keep class io.ktor.client.engine.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.debug.**

# ---------------------------------------------------------------------------
# Keep the Android entry point stable (also referenced from AndroidManifest.xml).
# ---------------------------------------------------------------------------
-keep class com.example.my_kmp_project.MainActivity { *; }
