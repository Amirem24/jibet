# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.jibet.app.**$$serializer { *; }
-keepclassmembers class com.jibet.app.** { *** Companion; }
-keepclasseswithmembers class com.jibet.app.** { kotlinx.serialization.KSerializer serializer(...); }
