# Gson DTOs are read by reflection: keep their fields if minify is enabled.
-keep class com.example.neurozen_front.data.remote.** { *; }
-keep class com.example.neurozen_front.domain.model.** { *; }
-keepattributes Signature, *Annotation*
