-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**
# Glide optionally depends on AppCompat
-dontwarn androidx.appcompat.**

-keepnames class * implements android.os.Parcelable {
   public static final ** CREATOR;
}

# attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
# Keep line numbers
-keepattributes SourceFile,LineNumberTable

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, replace classes with those containing named companion objects.
-keepattributes InnerClasses # Needed for `getDeclaredClasses`.

-if @kotlinx.serialization.Serializable class com.skydoves.pokedex.core.model.Pokemon
{
    static **$* *;
}
-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
    static <1>$$serializer INSTANCE;
}

# Keep both serializer and serializable classes to save the attribute InnerClasses
-keepclasseswithmembers, allowshrinking, allowobfuscation, allowaccessmodification class com.skydoves.pokedex.core.model.Pokemon
{
    *;
}
