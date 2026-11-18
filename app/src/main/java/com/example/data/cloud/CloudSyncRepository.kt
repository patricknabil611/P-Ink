package com.example.data.cloud

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.data.TodoRepository
import com.example.data.TodoTask
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class RemoteTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val text: String = "",
    val completed: Boolean = false,
    val clientCreatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RemoteNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val clientCreatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTasks(userId: String): Flow<List<RemoteTask>> =
        db.collection("users").document(userId).collection("tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RemoteTask::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }

    fun observeNotes(userId: String): Flow<List<RemoteNote>> =
        db.collection("users").document(userId).collection("notes")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RemoteNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }

    suspend fun upsertTask(userId: String, task: TodoTask): Result<Unit> {
        val safeText = task.text.trim().take(TodoRepository.MAX_TASK_TEXT_LENGTH)
        val path = "users/$userId/tasks/${task.remoteId}"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(VALID_ID_REGEX.matches(task.remoteId) && task.remoteId.length <= 128) {
                "Invalid remote task id"
            }
            require(safeText.isNotEmpty()) { "Task text cannot be empty" }

            val docRef = db.collection("users").document(uid)
                .collection("tasks").document(task.remoteId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "text" to safeText,
                        "completed" to task.isCompleted,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "text" to safeText,
                        "completed" to task.isCompleted,
                        "clientCreatedAt" to task.createdAt.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, remoteId: String): Result<Unit> {
        val path = "users/$userId/tasks/$remoteId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(VALID_ID_REGEX.matches(remoteId) && remoteId.length <= 128) {
                "Invalid remote task id"
            }
            db.collection("users").document(uid)
                .collection("tasks").document(remoteId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertNote(userId: String, note: NoteEntity): Result<Unit> {
        val safeContent = note.content.trim().take(NoteRepository.MAX_CONTENT_LENGTH)
        val safeTitle = note.title.trim().take(NoteRepository.MAX_TITLE_LENGTH)
        val path = "users/$userId/notes/${note.remoteId}"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(VALID_ID_REGEX.matches(note.remoteId) && note.remoteId.length <= 128) {
                "Invalid remote note id"
            }
            require(safeContent.isNotEmpty()) { "Note content cannot be empty" }

            val docRef = db.collection("users").document(uid)
                .collection("notes").document(note.remoteId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "content" to safeContent,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "content" to safeContent,
                        "clientCreatedAt" to note.createdAt.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, remoteId: String): Result<Unit> {
        val path = "users/$userId/notes/$remoteId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(VALID_ID_REGEX.matches(remoteId) && remoteId.length <= 128) {
                "Invalid remote note id"
            }
            db.collection("users").document(uid)
                .collection("notes").document(remoteId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun pushAllLocal(
        userId: String,
        tasks: List<TodoTask>,
        notes: List<NoteEntity>,
    ): Result<Unit> {
        return try {
            requireAuthenticatedUser(userId)
            val remoteTasks = db.collection("users").document(userId).collection("tasks")
                .get().await()
                .toObjects(RemoteTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .associateBy { it.id }

            for (task in tasks) {
                val safeText = task.text.trim().take(TodoRepository.MAX_TASK_TEXT_LENGTH)
                if (safeText.isEmpty()) continue
                val remote = remoteTasks[task.remoteId]
                val remoteUpdatedMs = (remote?.updatedAt ?: remote?.createdAt)?.toDate()?.time ?: 0L
                if (remote == null || task.updatedAt > remoteUpdatedMs + 1_500L) {
                    upsertTask(userId, task).getOrThrow()
                }
            }

            val remoteNotes = db.collection("users").document(userId).collection("notes")
                .get().await()
                .toObjects(RemoteNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .associateBy { it.id }

            for (note in notes) {
                val safeContent = note.content.trim().take(NoteRepository.MAX_CONTENT_LENGTH)
                if (safeContent.isEmpty()) continue
                val remote = remoteNotes[note.remoteId]
                val remoteUpdatedMs = (remote?.updatedAt ?: remote?.createdAt)?.toDate()?.time ?: 0L
                if (remote == null || note.updatedAt > remoteUpdatedMs + 1_500L) {
                    upsertNote(userId, note).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            Result.failure(e)
        }
    }

    private fun requireAuthenticatedUser(expectedUserId: String): String {
        val currentUid = auth.currentUser?.uid
            ?: error("User must be signed in with Google before accessing cloud sync.")
        check(currentUid == expectedUserId) {
            "Signed-in user changed; refusing cross-user cloud operation."
        }
        return currentUid
    }

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
