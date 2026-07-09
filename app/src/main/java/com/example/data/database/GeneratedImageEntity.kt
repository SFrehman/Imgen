package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val imageUrl: String, // Can be a local URI, path, base64 data, or custom reference
    val referenceImageUrl: String? = null,
    val characterId: Int? = null,
    val characterName: String? = null,
    val modelUsed: String = "Nanobana v3.2-Consistent",
    val resolution: String = "1024x1024",
    val seed: Long,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
