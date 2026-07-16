package com.saad.tvcast.core.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.saad.tvcast.core.common.LocalMediaItem
import com.saad.tvcast.core.common.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val COLUMN_DURATION = "duration"
        const val COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name"
    }

    suspend fun queryVideos(): List<LocalMediaItem> = queryMedia(MediaKind.Video)
    suspend fun queryPhotos(): List<LocalMediaItem> = queryMedia(MediaKind.Photo)
    suspend fun queryMusic(): List<LocalMediaItem> = queryMedia(MediaKind.Music)

    private suspend fun queryMedia(kind: MediaKind): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (collection, projection) = when (kind) {
            MediaKind.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
            MediaKind.Photo -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            MediaKind.Music -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            )
            MediaKind.WebVideo -> return@withContext emptyList()
        }

        val sort = when (kind) {
            MediaKind.Video -> "${MediaStore.Video.Media.DATE_ADDED} DESC"
            MediaKind.Photo -> "${MediaStore.Images.Media.DATE_ADDED} DESC"
            MediaKind.Music -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            MediaKind.WebVideo -> null
        }

        resolver.query(collection, projection, null, null, sort)?.use { cursor ->
            buildList {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val durationColumn = cursor.getColumnIndex(COLUMN_DURATION)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val folderColumn = cursor.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        LocalMediaItem(
                            id = id.toString(),
                            uri = ContentUris.withAppendedId(collection, id),
                            displayName = cursor.getString(nameColumn).orEmpty(),
                            kind = kind,
                            mimeType = cursor.getString(mimeColumn),
                            sizeBytes = cursor.getLong(sizeColumn),
                            durationMillis = durationColumn.takeIf { it >= 0 }?.let { cursor.getLong(it) },
                            dateAddedMillis = cursor.getLong(dateColumn) * 1000,
                            album = albumColumn.takeIf { it >= 0 }?.let { cursor.getString(it) },
                            artist = artistColumn.takeIf { it >= 0 }?.let { cursor.getString(it) },
                            folder = folderColumn.takeIf { it >= 0 }?.let { cursor.getString(it) }
                        )
                    )
                }
            }
        } ?: emptyList()
    }
}
