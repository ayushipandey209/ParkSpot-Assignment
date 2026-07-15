# Keep Firestore model classes (they are deserialized reflectively)
-keepclassmembers class com.zobaze.parkspot.data.model.** {
    <init>();
    <fields>;
}
