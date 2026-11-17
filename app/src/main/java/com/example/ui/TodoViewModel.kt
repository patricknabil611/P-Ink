package com.example.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.HandwritingRecognizer
import com.example.ai.RecognitionResult
import com.example.data.AppDatabase
import com.example.data.TodoRepository
import com.example.data.TodoTask
import com.example.ink.InkBitmapRenderer
import com.example.ink.InkStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConversionState {
    data object Idle : ConversionState
    data object AutoConvertPending : ConversionState
    data object Converting : ConversionState
    data object Success : ConversionState
    data class Error(
        @StringRes val messageRes: Int? = null,
        val customMessage: String? = null,
    ) : ConversionState
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TodoRepository by lazy {
        val dao = AppDatabase.getInstance(application).todoDao()
        TodoRepository(dao)
    }

    private val recognizer: HandwritingRecognizer by lazy {
        HandwritingRecognizer(application.applicationContext)
    }

    val tasks: StateFlow<List<TodoTask>> = repository.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    private val _strokes = MutableStateFlow<List<InkStroke>>(emptyList())
    val strokes: StateFlow<List<InkStroke>> = _strokes.asStateFlow()

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    private val _editingTask = MutableStateFlow<TodoTask?>(null)
    val editingTask: StateFlow<TodoTask?> = _editingTask.asStateFlow()

    private var canvasSize = IntSize(1080, 480)
    private var autoConvertJob: Job? = null
    private var convertJob: Job? = null
    private var lastDeletedTask: TodoTask? = null

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
        scheduleAutoConvert()
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
            scheduleAutoConvert()
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

    fun onAddTask() {
        val text = _draftText.value.trim()
        if (text.isEmpty()) return
        autoConvertJob?.cancel()
        convertJob?.cancel()
        viewModelScope.launch {
            repository.addTask(text)
            _draftText.value = ""
            _strokes.value = emptyList()
            _conversionState.value = ConversionState.Idle
        }
    }

    fun onToggleTask(task: TodoTask) {
        viewModelScope.launch {
            repository.toggleCompleted(task)
        }
    }

    fun onDeleteTask(task: TodoTask) {
        lastDeletedTask = task
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun onUndoDelete() {
        val taskToRestore = lastDeletedTask ?: return
        lastDeletedTask = null
        viewModelScope.launch {
            repository.restoreTask(taskToRestore)
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
                repository.updateText(target, trimmed)
            }
        }
    }

    private fun scheduleAutoConvert() {
        autoConvertJob?.cancel()
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
                            messageRes = R.string.error_generic,
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

    companion object {
        private const val AUTO_CONVERT_DELAY_MS = 1_500L
    }
}
