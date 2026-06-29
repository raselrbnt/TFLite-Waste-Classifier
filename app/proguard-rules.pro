# Room database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.limits.Limit

# TensorFlow Lite rules
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep our data model and ML classifier entities from being stripped/obfuscated
-keep class com.example.sampahclassifierapp.ml.WasteClassifier$** { *; }
-keep class com.example.sampahclassifierapp.data.** { *; }
