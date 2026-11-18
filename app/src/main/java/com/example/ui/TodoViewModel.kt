package com.example.ui

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.HandwritingRecognizer
import com.example.ai.RecognitionResult
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.AppSettingsRepository
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.data.TodoRepository
import com.example.data.TodoTask
import com.example.data.WritingPadSize
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.GoogleSignInResult
import com.example.ink.InkBitmapRenderer
import com.example.ink.InkStroke
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    WRITE,
    NOTES,
    SETTINGS,
}

sealed interface ConversionState {
    data object Idle : ConversionState
    data object AutoConvertPending : ConversionState
    data object Converting : ConversionState
    data object Success : ConversionState
    data class Error(
        @param:StringRes val messageRes: Int? = null,
        val customMessage: String? = null,
    ) : ConversionState
}

data class SyncUiState(
    val userUid: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val isSigningIn: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTimeMs: Long? = null,
    val statusMessage: String? = null,
    @param:StringRes val errorRes: Int? = null,
    val customError: String? = null,
) {
    val isSignedIn: Boolean get() = !userUid.isNullOrBlank()
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(application)
    }

    private val repository: TodoRepository by lazy {
        TodoRepository(database.todoDao())
    }

    private val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    private val settingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(application)
    }

    private val recognizer: HandwritingRecognizer by lazy {
        HandwritingRecognizer(application.applicationContext)
    }

    private val isFirebaseAvailable: Boolean by lazy {
        runCatching {
            FirebaseApp.getApps(application).isNotEmpty() ||
                FirebaseApp.initializeApp(application) != null
        }.getOrDefault(false)
    }

    private val googleAuthHelper: GoogleAuthHelper? by lazy {
        if (isFirebaseAvailable) runCatching { GoogleAuthHelper() }.getOrNull() else null
    }

    private val cloudSyncRepository: CloudSyncRepository? by lazy {
        if (isFirebaseAvailable) {
            runCatching { CloudSyncRepository(application.applicationContext) }.getOrNull()
        } else {
            null
        }
    }

    val tasks: StateFlow<List<TodoTask>> = repository.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    val notes: StateFlow<List<NoteEntity>> = noteRepository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AppSettings(),
    )

    private val _currentTab = MutableStateFlow(AppTab.WRITE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _strokes = MutableStateFlow<List<InkStroke>>(emptyList())
    val strokes: StateFlow<List<InkStroke>> = _strokes.asStateFlow()

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    private val _editingTask = MutableStateFlow<TodoTask?>(null)
    val editingTask: StateFlow<TodoTask?> = _editingTask.asStateFlow()

    private val _isPaletteExpanded = MutableStateFlow(false)
    val isPaletteExpanded: StateFlow<Boolean> = _isPaletteExpanded.asStateFlow()

    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private var canvasSize = IntSize(1080, 480)
    private var autoConvertJob: Job? = null
    private var convertJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var activeSyncUid: String? = null
    private var lastDeletedTask: TodoTask? = null
    private var lastDeletedNote: NoteEntity? = null

    init {
        observeAuthAndSync()
    }

    private fun observeAuthAndSync() {
        val helper = googleAuthHelper ?: return
        viewModelScope.launch {
            helper.authState.collect { user ->
                updateUserAndManageSync(user)
            }
        }
    }

    private suspend fun updateUserAndManageSync(user: FirebaseUser?) {
        if (user == null) {
            stopCloudSyncSafely()
            _syncState.value = SyncUiState()
        } else {
            val uid = user.uid
            _syncState.value = _syncState.value.copy(
                userUid = uid,
                userName = user.displayName,
                userEmail = user.email,
                isSigningIn = false,
                errorRes = null,
                customError = null,
            )
            if (settings.value.cloudSyncEnabled && activeSyncUid != uid) {
                startCloudSync(uid)
            }
        }
    }

    private fun startCloudSync(uid: String) {
        val cloudRepo = cloudSyncRepository ?: return
        cloudSyncJob?.cancel()
        activeSyncUid = uid
        cloudSyncJob = viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isSyncing = true, errorRes = null, customError = null)
            val pushResult = cloudRepo.pushAllLocal(
                userId = uid,
                tasks = repository.getAllTasksOnce(),
                notes = noteRepository.getAllNotesOnce(),
            )
            if (pushResult.isFailure) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    customError = pushResult.exceptionOrNull()?.localizedMessage,
                )
            } else {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTimeMs = System.currentTimeMillis(),
                )
            }

            launch {
                cloudRepo.observeTasks(uid)
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        val rootError = fsError ?: error
                        _syncState.value = _syncState.value.copy(
                            isSyncing = false,
                            customError = rootError.localizedMessage,
                        )
                    }
                    .collect { remoteTasks ->
                        for (remote in remoteTasks) {
                            val updatedMs = (remote.updatedAt ?: remote.createdAt)?.toDate()?.time
                                ?: remote.clientCreatedAt
                            if (remote.id.isNotBlank()) {
                                repository.upsertFromRemote(
                                    remoteId = remote.id,
                                    text = remote.text,
                                    completed = remote.completed,
                                    clientCreatedAt = remote.clientCreatedAt,
                                    remoteUpdatedAt = updatedMs,
                                )
                            }
                        }
                        _syncState.value = _syncState.value.copy(lastSyncTimeMs = System.currentTimeMillis())
                    }
            }

            launch {
                cloudRepo.observeNotes(uid)
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        val rootError = fsError ?: error
                        _syncState.value = _syncState.value.copy(
                            isSyncing = false,
                            customError = rootError.localizedMessage,
                        )
                    }
                    .collect { remoteNotes ->
                        for (remote in remoteNotes) {
                            val updatedMs = (remote.updatedAt ?: remote.createdAt)?.toDate()?.time
                                ?: remote.clientCreatedAt
                            if (remote.id.isNotBlank()) {
                                noteRepository.upsertFromRemote(
                                    remoteId = remote.id,
                                    title = remote.title,
                                    content = remote.content,
                                    clientCreatedAt = remote.clientCreatedAt,
                                    remoteUpdatedAt = updatedMs,
                                )
                            }
                        }
                        _syncState.value = _syncState.value.copy(lastSyncTimeMs = System.currentTimeMillis())
                    }
            }
        }
    }

    private suspend fun stopCloudSyncSafely() {
        withContext(NonCancellable) {
            val job = cloudSyncJob
            cloudSyncJob = null
            activeSyncUid = null
            job?.cancel()
            job?.join()
        }
    }

    // --- Navigation ---

    fun onSelectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun onTogglePaletteExpanded() {
        _isPaletteExpanded.value = !_isPaletteExpanded.value
    }

    // --- Ink Canvas Actions ---

    fun onCanvasSizeChanged(size: IntSize) {
        if (size.width > 0 && size.height > 0) {
            canvasSize = size
        }
    }

    fun onStrokeStarted() {
        autoConvertJob?.cancel()
        convertJob?.cancel()
        if (_conversionState.value is ConversionState.Converting ||
            _conversionState.value is ConversionState.AutoConvertPending ||
            _conversionState.value is ConversionState.Error
        ) {
            _conversionState.value = ConversionState.Idle
        }
    }

    fun onStrokeCompleted(stroke: InkStroke) {
        if (stroke.points.isEmpty()) return
        _strokes.value = _strokes.value + stroke
        scheduleAutoConvertIfEnabled()
    }

    fun onUndoStroke() {
        autoConvertJob?.cancel()
        convertJob?.cancel()
        val current = _strokes.value
        if (current.isEmpty()) return
        val updated = current.dropLast(1)
        _strokes.value = updated
        if (updated.isEmpty()) {
            _conversionState.value = ConversionState.Idle
        } else {
            scheduleAutoConvertIfEnabled()
        }
    }

    fun onEraseStrokes(indicesToRemove: Set<Int>) {
        if (indicesToRemove.isEmpty()) return
        val current = _strokes.value
        if (current.isEmpty()) return
        val updated = current.filterIndexed { index, _ -> index !in indicesToRemove }
        if (updated.size == current.size) return

        autoConvertJob?.cancel()
        convertJob?.cancel()
        _strokes.value = updated
        if (updated.isEmpty()) {
            _conversionState.value = ConversionState.Idle
        } else {
            scheduleAutoConvertIfEnabled()
        }
    }

    fun onClearCanvas() {
        autoConvertJob?.cancel()
        convertJob?.cancel()
        _strokes.value = emptyList()
        _conversionState.value = ConversionState.Idle
    }

    fun onConvertNow() {
        autoConvertJob?.cancel()
        if (_strokes.value.isEmpty()) return
        convertHandwriting()
    }

    fun onDismissError() {
        if (_conversionState.value is ConversionState.Error) {
            _conversionState.value = ConversionState.Idle
        }
    }

    fun onDraftTextChanged(newText: String) {
        _draftText.value = newText
    }

    fun onClearDraftText() {
        _draftText.value = ""
    }

    // --- Task CRUD ---

    fun onAddTask() {
        val text = _draftText.value.trim()
        if (text.isEmpty()) return
        autoConvertJob?.cancel()
        convertJob?.cancel()
        viewModelScope.launch {
            val inserted = repository.addTask(text)
            _draftText.value = ""
            _strokes.value = emptyList()
            _conversionState.value = ConversionState.Idle
            if (inserted != null) {
                syncUpsertTask(inserted)
            }
        }
    }

    fun onAddTaskFromText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val inserted = repository.addTask(trimmed)
            if (inserted != null) {
                syncUpsertTask(inserted)
            }
        }
    }

    fun onToggleTask(task: TodoTask) {
        viewModelScope.launch {
            val updated = repository.toggleCompleted(task)
            syncUpsertTask(updated)
        }
    }

    fun onDeleteTask(task: TodoTask) {
        lastDeletedTask = task
        viewModelScope.launch {
            repository.deleteTask(task)
            syncDeleteTask(task.remoteId)
        }
    }

    fun onUndoDelete() {
        val taskToRestore = lastDeletedTask ?: return
        lastDeletedTask = null
        viewModelScope.launch {
            val restored = repository.restoreTask(taskToRestore)
            syncUpsertTask(restored)
        }
    }

    fun onStartEditTask(task: TodoTask) {
        _editingTask.value = task
    }

    fun onDismissEditTask() {
        _editingTask.value = null
    }

    fun onSaveEditTask(newText: String) {
        val target = _editingTask.value ?: return
        val trimmed = newText.trim()
        _editingTask.value = null
        if (trimmed.isNotEmpty() && trimmed != target.text) {
            viewModelScope.launch {
                val updated = repository.updateText(target, trimmed)
                if (updated != null) {
                    syncUpsertTask(updated)
                }
            }
        }
    }

    // --- Notes CRUD ---

    fun onNoteSearchQueryChanged(query: String) {
        _noteSearchQuery.value = query
    }

    fun onSaveDraftAsNote(onSaved: () -> Unit = {}) {
        val content = _draftText.value.trim()
        if (content.isEmpty()) return
        autoConvertJob?.cancel()
        convertJob?.cancel()
        viewModelScope.launch {
            val saved = noteRepository.saveNote(content = content)
            _draftText.value = ""
            _strokes.value = emptyList()
            _conversionState.value = ConversionState.Idle
            if (saved != null) {
                onSaved()
                syncUpsertNote(saved)
            }
        }
    }

    fun onStartCreateNote() {
        _editingNote.value = NoteEntity(id = -1L, title = "", content = "")
    }

    fun onStartEditNote(note: NoteEntity) {
        _editingNote.value = note
    }

    fun onDismissEditNote() {
        _editingNote.value = null
    }

    fun onSaveNoteDialog(title: String, content: String) {
        val target = _editingNote.value ?: return
        val trimmedContent = content.trim()
        _editingNote.value = null
        if (trimmedContent.isEmpty()) return

        viewModelScope.launch {
            val saved = if (target.id <= 0L) {
                noteRepository.saveNote(title = title, content = trimmedContent)
            } else {
                noteRepository.updateNote(target, newTitle = title, newContent = trimmedContent)
            }
            if (saved != null) {
                syncUpsertNote(saved)
            }
        }
    }

    fun onDeleteNote(note: NoteEntity) {
        lastDeletedNote = note
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            syncDeleteNote(note.remoteId)
        }
    }

    fun onUndoDeleteNote() {
        val noteToRestore = lastDeletedNote ?: return
        lastDeletedNote = null
        viewModelScope.launch {
            val restored = noteRepository.restoreNote(noteToRestore)
            syncUpsertNote(restored)
        }
    }

    fun onLoadNoteToDraft(note: NoteEntity) {
        _draftText.value = note.content
        _currentTab.value = AppTab.WRITE
    }

    // --- Settings & Color Palettes ---

    fun onSelectPadBackground(optionId: String) {
        viewModelScope.launch {
            settingsRepository.setPadBackground(optionId)
        }
    }

    fun onSelectPenColor(optionId: String) {
        viewModelScope.launch {
            settingsRepository.setPenColor(optionId)
        }
    }

    fun onSetShowGuideLines(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowGuideLines(enabled)
        }
    }

    fun onSetAutoConvertEnabled(enabled: Boolean) {
        if (!enabled && _conversionState.value is ConversionState.AutoConvertPending) {
            autoConvertJob?.cancel()
            _conversionState.value = ConversionState.Idle
        }
        viewModelScope.launch {
            settingsRepository.setAutoConvertEnabled(enabled)
        }
    }

    fun onSetWritingPadSize(size: WritingPadSize) {
        viewModelScope.launch {
            settingsRepository.setWritingPadSize(size)
        }
    }

    fun onSetCompactLists(compact: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCompactLists(compact)
        }
    }

    fun onSetCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCloudSyncEnabled(enabled)
            val uid = _syncState.value.userUid
            if (enabled && !uid.isNullOrBlank()) {
                startCloudSync(uid)
            } else if (!enabled) {
                stopCloudSyncSafely()
            }
        }
    }

    // --- Google Sign-In & Cloud Sync ---

    fun onSignInWithGoogle(activityContext: Context) {
        val helper = googleAuthHelper ?: return
        if (_syncState.value.isSigningIn) return
        _syncState.value = _syncState.value.copy(
            isSigningIn = true,
            errorRes = null,
            customError = null,
            statusMessage = null,
        )
        viewModelScope.launch {
            when (val result = helper.signInInteractive(activityContext)) {
                is GoogleSignInResult.Success -> {
                    updateUserAndManageSync(result.user)
                }
                GoogleSignInResult.Cancelled -> {
                    _syncState.value = _syncState.value.copy(
                        isSigningIn = false,
                        errorRes = R.string.error_signin_cancelled,
                    )
                }
                GoogleSignInResult.NoCredentials -> {
                    _syncState.value = _syncState.value.copy(
                        isSigningIn = false,
                        errorRes = R.string.error_signin_no_credentials,
                    )
                }
                is GoogleSignInResult.Failure -> {
                    _syncState.value = _syncState.value.copy(
                        isSigningIn = false,
                        customError = result.message,
                    )
                }
            }
        }
    }

    fun onSignOut() {
        val helper = googleAuthHelper ?: return
        viewModelScope.launch {
            stopCloudSyncSafely()
            helper.signOut(getApplication())
            _syncState.value = SyncUiState()
        }
    }

    fun onSyncNow(onDone: () -> Unit = {}) {
        val uid = _syncState.value.userUid ?: return
        val cloudRepo = cloudSyncRepository ?: return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isSyncing = true, errorRes = null, customError = null)
            val result = cloudRepo.pushAllLocal(
                userId = uid,
                tasks = repository.getAllTasksOnce(),
                notes = noteRepository.getAllNotesOnce(),
            )
            _syncState.value = if (result.isSuccess) {
                onDone()
                _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTimeMs = System.currentTimeMillis(),
                )
            } else {
                _syncState.value.copy(
                    isSyncing = false,
                    customError = result.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    // --- Internal Cloud & OCR Helpers ---

    private suspend fun syncUpsertTask(task: TodoTask) {
        val uid = googleAuthHelper?.currentUser?.uid ?: return
        if (!settings.value.cloudSyncEnabled) return
        cloudSyncRepository?.upsertTask(uid, task)
    }

    private suspend fun syncDeleteTask(remoteId: String) {
        val uid = googleAuthHelper?.currentUser?.uid ?: return
        if (!settings.value.cloudSyncEnabled) return
        cloudSyncRepository?.deleteTask(uid, remoteId)
    }

    private suspend fun syncUpsertNote(note: NoteEntity) {
        val uid = googleAuthHelper?.currentUser?.uid ?: return
        if (!settings.value.cloudSyncEnabled) return
        cloudSyncRepository?.upsertNote(uid, note)
    }

    private suspend fun syncDeleteNote(remoteId: String) {
        val uid = googleAuthHelper?.currentUser?.uid ?: return
        if (!settings.value.cloudSyncEnabled) return
        cloudSyncRepository?.deleteNote(uid, remoteId)
    }

    private fun scheduleAutoConvertIfEnabled() {
        autoConvertJob?.cancel()
        if (!settings.value.autoConvertEnabled) {
            _conversionState.value = ConversionState.Idle
            return
        }
        _conversionState.value = ConversionState.AutoConvertPending
        autoConvertJob = viewModelScope.launch {
            delay(AUTO_CONVERT_DELAY_MS)
            if (_strokes.value.isNotEmpty()) {
                convertHandwriting()
            } else {
                _conversionState.value = ConversionState.Idle
            }
        }
    }

    private fun convertHandwriting() {
        val strokesSnapshot = _strokes.value
        if (strokesSnapshot.isEmpty()) return

        convertJob?.cancel()
        convertJob = viewModelScope.launch {
            _conversionState.value = ConversionState.Converting
            val bitmap = withContext(Dispatchers.Default) {
                InkBitmapRenderer.renderToBitmap(
                    strokes = strokesSnapshot,
                    canvasWidth = canvasSize.width,
                    canvasHeight = canvasSize.height,
                )
            }
            if (bitmap == null) {
                _conversionState.value = ConversionState.Idle
                return@launch
            }

            try {
                when (val result = recognizer.recognize(bitmap)) {
                    is RecognitionResult.Success -> {
                        _draftText.value = result.text
                        _conversionState.value = ConversionState.Success
                    }
                    RecognitionResult.NoInternet -> {
                        _conversionState.value = ConversionState.Error(
                            messageRes = R.string.error_no_internet
                        )
                    }
                    RecognitionResult.EmptyOutput -> {
                        _conversionState.value = ConversionState.Error(
                            messageRes = R.string.error_empty_recognition
                        )
                    }
                    RecognitionResult.TimedOut -> {
                        _conversionState.value = ConversionState.Error(
                            messageRes = R.string.error_timeout
                        )
                    }
                    is RecognitionResult.Failure -> {
                        _conversionState.value = ConversionState.Error(
                            messageRes = null,
                            customMessage = result.message,
                        )
                    }
                }
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cloudSyncJob?.cancel()
    }

    companion object {
        private const val AUTO_CONVERT_DELAY_MS = 1_500L
    }
}
