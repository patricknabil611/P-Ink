package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(private val noteDao: NoteDao) {
    val notes: Flow<List<NoteEntity>> = noteDao.observeAllNotes()

    suspend fun saveNote(title: String = "", content: String): NoteEntity? {
        val trimmedContent = content.trim().take(MAX_CONTENT_LENGTH)
        if (trimmedContent.isEmpty()) return null

        val resolvedTitle = normalizeTitle(title, trimmedContent)
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            remoteId = UUID.randomUUID().toString(),
            title = resolvedTitle,
            content = trimmedContent,
            createdAt = now,
            updatedAt = now,
        )
        val insertedId = noteDao.insertNote(entity)
        return entity.copy(id = insertedId)
    }

    suspend fun updateNote(note: NoteEntity, newTitle: String, newContent: String): NoteEntity? {
        val trimmedContent = newContent.trim().take(MAX_CONTENT_LENGTH)
        if (trimmedContent.isEmpty()) return null

        val resolvedTitle = normalizeTitle(newTitle, trimmedContent)
        val updated = note.copy(
            title = resolvedTitle,
            content = trimmedContent,
            updatedAt = System.currentTimeMillis(),
        )
        noteDao.updateNote(updated)
        return updated
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    suspend fun restoreNote(note: NoteEntity): NoteEntity {
        val insertedId = noteDao.insertNote(note)
        return note.copy(id = insertedId)
    }

    suspend fun getAllNotesOnce(): List<NoteEntity> = noteDao.getAllNotesOnce()

    suspend fun upsertFromRemote(
        remoteId: String,
        title: String,
        content: String,
        clientCreatedAt: Long,
        remoteUpdatedAt: Long,
    ) {
        val existing = noteDao.getByRemoteId(remoteId)
        val safeContent = content.trim().take(MAX_CONTENT_LENGTH)
        if (safeContent.isEmpty()) return
        val safeTitle = title.trim().take(MAX_TITLE_LENGTH)

        if (existing == null) {
            noteDao.insertNote(
                NoteEntity(
                    remoteId = remoteId,
                    title = safeTitle.ifEmpty { deriveAutoTitle(safeContent) },
                    content = safeContent,
                    createdAt = if (clientCreatedAt > 0L) clientCreatedAt else remoteUpdatedAt,
                    updatedAt = remoteUpdatedAt,
                )
            )
        } else if (remoteUpdatedAt > existing.updatedAt + SYNC_DRIFT_GRACE_MS) {
            noteDao.updateNote(
                existing.copy(
                    title = safeTitle.ifEmpty { deriveAutoTitle(safeContent) },
                    content = safeContent,
                    updatedAt = remoteUpdatedAt,
                )
            )
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 200
        const val MAX_CONTENT_LENGTH = 20_000
        private const val AUTO_TITLE_MAX = 56
        private const val SYNC_DRIFT_GRACE_MS = 1_500L

        fun deriveAutoTitle(content: String): String {
            val firstLine = content.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                ?: return ""
            return if (firstLine.length <= AUTO_TITLE_MAX) {
                firstLine
            } else {
                firstLine.take(AUTO_TITLE_MAX - 1).trimEnd() + "…"
            }
        }

        fun normalizeTitle(title: String, content: String): String {
            val cleaned = title.trim().take(MAX_TITLE_LENGTH)
            return if (cleaned.isNotEmpty()) cleaned else deriveAutoTitle(content)
        }
    }
}
