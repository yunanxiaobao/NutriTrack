# R8 / ProGuard rules for NutriTrack release builds

# Keep Gson-serialized wire DTOs, Room entities, and domain models
-keep class com.songshawn.nutritrack.data.network.fruityvice.** { *; }
-keep class com.songshawn.nutritrack.data.network.genai.** { *; }
-keep class com.songshawn.nutritrack.data.room.entity.** { *; }
-keep class com.songshawn.nutritrack.data.room.dao.** { *; }
-keep class com.songshawn.nutritrack.data.models.** { *; }
-keep class com.songshawn.nutritrack.domain.agent.ToolCallTraceStep { *; }
-keep class com.songshawn.nutritrack.domain.agent.DetectedMealPlate { *; }
-keep class com.songshawn.nutritrack.domain.agent.DetectedMealItem { *; }

# Keep generic signatures for Gson TypeToken reflection
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit service interfaces
-keep interface com.songshawn.nutritrack.data.network.** { *; }
