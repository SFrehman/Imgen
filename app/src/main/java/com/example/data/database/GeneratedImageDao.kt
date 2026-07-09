package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images ORDER BY createdAt DESC")
    fun getAllImagesFlow(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Int): GeneratedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity): Long

    @Update
    suspend fun updateImage(image: GeneratedImageEntity)

    @Delete
    suspend fun deleteImage(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images")
    suspend fun deleteAllImages()
}
