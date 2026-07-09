package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val straightFaceUri: String? = null,
    val rightFaceUri: String? = null,
    val leftFaceUri: String? = null,
    val downFaceUri: String? = null,
    val upFaceUri: String? = null,
    val seed: Long = (100000..999999).random().toLong(),
    val stabilityWeight: Float = 0.85f,
    val createdAt: Long = System.currentTimeMillis()
)
