# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **.*$Companion { kotlinx.serialization.KSerializer serializer(...); }
