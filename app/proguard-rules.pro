# Proguard rules for Room, Hilt, Coroutines, and Gson
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn com.google.crypto.tink.**
