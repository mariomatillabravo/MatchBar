# R8 (release). Retrofit, OkHttp y kotlinx-serialization traen sus propias
# reglas dentro de las librerías; aquí solo va lo específico de la app.

# Modelos de la API: se (de)serializan con kotlinx-serialization.
-keep class com.matchbar.app.data.model.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Conserva fichero y línea en las trazas de error: con el código ofuscado, un
# fallo en producción sería imposible de localizar sin esto.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
