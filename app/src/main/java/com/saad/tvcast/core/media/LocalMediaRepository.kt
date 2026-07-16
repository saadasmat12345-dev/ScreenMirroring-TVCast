package com.saad.tvcast.core.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.saad.tvcast.R
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

        val selection = when (kind) {
            MediaKind.Music -> "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            else -> null
        }

        resolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            buildList {
                val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (idColumn < 0 || nameColumn < 0) return@buildList

                val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val durationColumn = cursor.getColumnIndex(COLUMN_DURATION)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val folderColumn = cursor.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLongOrNull(idColumn) ?: continue
                    add(
                        LocalMediaItem(
                            id = id.toString(),
                            uri = ContentUris.withAppendedId(collection, id),
                            displayName = cursor.getStringOrNull(nameColumn).orEmpty()
                                .ifBlank { context.getString(R.string.untitled_media) },
                            kind = kind,
                            mimeType = cursor.getStringOrNull(mimeColumn),
                            sizeBytes = cursor.getLongOrNull(sizeColumn) ?: 0L,
                            durationMillis = cursor.getLongOrNull(durationColumn),
                            dateAddedMillis = (cursor.getLongOrNull(dateColumn) ?: 0L) * 1000,
                            album = cursor.getStringOrNull(albumColumn),
                            artist = cursor.getStringOrNull(artistColumn),
                            folder = cursor.getStringOrNull(folderColumn)
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (index >= 0 && !isNull(index)) getString(index) else null

    private fun Cursor.getLongOrNull(index: Int): Long? =
        if (index >= 0 && !isNull(index)) getLong(index) else null
}
