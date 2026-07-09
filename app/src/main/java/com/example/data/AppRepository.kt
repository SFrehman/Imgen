package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.database.CharacterEntity
import com.example.data.database.GeneratedImageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class AppRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val characterDao = database.characterDao()
    private val generatedImageDao = database.generatedImageDao()

    val allCharacters: Flow<List<CharacterEntity>> = characterDao.getAllCharactersFlow()
    val allImages: Flow<List<GeneratedImageEntity>> = generatedImageDao.getAllImagesFlow()

    suspend fun insertCharacter(character: CharacterEntity): Long {
        return characterDao.insertCharacter(character)
    }

    suspend fun updateCharacter(character: CharacterEntity) {
        characterDao.insertCharacter(character)
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        characterDao.deleteCharacter(character)
        // Also clean up local file URIs if any
        cleanupCharacterFiles(character)
    }

    suspend fun insertImage(image: GeneratedImageEntity): Long {
        return generatedImageDao.insertImage(image)
    }

    suspend fun updateImage(image: GeneratedImageEntity) {
        generatedImageDao.updateImage(image)
    }

    suspend fun deleteImage(image: GeneratedImageEntity) {
        generatedImageDao.deleteImage(image)
        try {
            val file = File(Uri.parse(image.imageUrl).path ?: "")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllData() {
        characterDao.deleteAllCharacters()
        generatedImageDao.deleteAllImages()
    }

    fun saveImageToInternalStorage(fileName: String, bytes: ByteArray): String {
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use {
            it.write(bytes)
        }
        return Uri.fromFile(file).toString()
    }

    private fun cleanupCharacterFiles(character: CharacterEntity) {
        val uris = listOfNotNull(
            character.straightFaceUri,
            character.rightFaceUri,
            character.leftFaceUri,
            character.downFaceUri,
            character.upFaceUri
        )
        for (uriStr in uris) {
            try {
                val file = File(Uri.parse(uriStr).path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
