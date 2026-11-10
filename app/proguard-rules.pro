# ---------------------------------------------------------------------------
# NeuroZen - R8 rules for the release build (isMinifyEnabled = true)
# ---------------------------------------------------------------------------

# Gson reads/writes these classes by reflection: keep class and field names.
#  - data.remote: Retrofit DTOs (Dtos.kt) and ErrorResponseDto parsed in ApiCall.kt
#  - domain.model: Session, serialized to DataStore by SessionRepositoryImpl
-keep class com.example.neurozen_front.data.remote.** { *; }
-keep class com.example.neurozen_front.domain.model.** { *; }

# Generic signatures and annotations (needed by Gson and Retrofit).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Retrofit with suspend functions (R8 full mode)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Readable crash stack traces
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
