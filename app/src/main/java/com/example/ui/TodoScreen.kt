package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.AppSettings
import com.example.data.TodoTask
import com.example.ink.InkStroke
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanOnContainer
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkOnContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseErrorContainer
import com.example.ui.theme.RoseOnErrorContainer
import com.example.ui.theme.SlateSurface
import com.example.util.TextDirectionUtil
import kotlinx.coroutines.launch

@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel(),
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val draftText by viewModel.draftText.collectAsStateWithLifecycle()
    val conversionState by viewModel.conversionState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val isPaletteExpanded by viewModel.isPaletteExpanded.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val noteSearchQuery by viewModel.noteSearchQuery.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val deletedTaskMsg = stringResource(R.string.snackbar_task_deleted)
    val deletedNoteMsg = stringResource(R.string.snackbar_note_deleted)
    val undoLabel = stringResource(R.string.snackbar_undo)
    val copiedDraftMsg = stringResource(R.string.snackbar_copied)
    val copiedNoteMsg = stringResource(R.string.snackbar_note_copied)
    val savedNoteMsg = stringResource(R.string.snackbar_note_saved)
    val syncDoneMsg = stringResource(R.string.snackbar_sync_done)

    BackHandler(enabled = currentTab != AppTab.WRITE) {
        viewModel.onSelectTab(AppTab.WRITE)
    }

    fun showTransientMessage(msg: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun copyToClipboard(text: String, message: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("P Ink", text))
        showTransientMessage(message)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        val useNavigationRail = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                PInkNavigationRail(
                    currentTab = currentTab,
                    onSelectTab = viewModel::onSelectTab,
                )
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        PInkBottomBar(
                            currentTab = currentTab,
                            onSelectTab = viewModel::onSelectTab,
                        )
                    }
                },
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    val isWideWriteLayout = maxWidth >= 700.dp
                    val padSizeSpec = settings.writingPadSize
                    val minHeightDp = if (maxHeight < 500.dp) 104.dp else padSizeSpec.minDp.dp
                    val portraitPadHeight = (maxHeight * padSizeSpec.heightFraction)
                        .coerceIn(minHeightDp, padSizeSpec.maxDp.dp)

                    when (currentTab) {
                        AppTab.WRITE -> {
                            WriteAndTasksTab(
                                isWideLayout = isWideWriteLayout,
                                portraitPadHeight = portraitPadHeight,
                                strokes = strokes,
                                draftText = draftText,
                                conversionState = conversionState,
                                tasks = tasks,
                                settings = settings,
                                isPaletteExpanded = isPaletteExpanded,
                                onTogglePalette = viewModel::onTogglePaletteExpanded,
                                onSelectPadBackground = viewModel::onSelectPadBackground,
                                onSelectPenColor = viewModel::onSelectPenColor,
                                onCanvasSizeChanged = viewModel::onCanvasSizeChanged,
                                onStrokeStarted = viewModel::onStrokeStarted,
                                onStrokeCompleted = viewModel::onStrokeCompleted,
                                onEraseStrokes = viewModel::onEraseStrokes,
                                onUndoStroke = viewModel::onUndoStroke,
                                onClearCanvas = viewModel::onClearCanvas,
                                onConvertNow = viewModel::onConvertNow,
                                onDismissError = viewModel::onDismissError,
                                onDraftTextChanged = viewModel::onDraftTextChanged,
                                onClearDraftText = viewModel::onClearDraftText,
                                onCopyDraftText = { copyToClipboard(draftText, copiedDraftMsg) },
                                onSaveDraftAsNote = {
                                    viewModel.onSaveDraftAsNote {
                                        showTransientMessage(savedNoteMsg)
                                    }
                                },
                                onAddTask = viewModel::onAddTask,
                                onToggleTask = viewModel::onToggleTask,
                                onEditTask = viewModel::onStartEditTask,
                                onDeleteTask = { task ->
                                    viewModel.onDeleteTask(task)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedTaskMsg,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.onUndoDelete()
                                        }
                                    }
                                },
                            )
                        }

                        AppTab.NOTES -> {
                            NotesTab(
                                notes = notes,
                                searchQuery = noteSearchQuery,
                                compactLists = settings.compactLists,
                                editingNote = editingNote,
                                onSearchQueryChanged = viewModel::onNoteSearchQueryChanged,
                                onCreateNewNote = viewModel::onStartCreateNote,
                                onOpenNote = viewModel::onStartEditNote,
                                onCopyNote = { note -> copyToClipboard(note.content, copiedNoteMsg) },
                                onDeleteNote = { note ->
                                    viewModel.onDeleteNote(note)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedNoteMsg,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.onUndoDeleteNote()
                                        }
                                    }
                                },
                                onDismissNoteDialog = viewModel::onDismissEditNote,
                                onSaveNoteDialog = viewModel::onSaveNoteDialog,
                                onLoadNoteToWrite = viewModel::onLoadNoteToDraft,
                                onAddNoteAsTask = { note ->
                                    viewModel.onAddTaskFromText(note.content)
                                    viewModel.onSelectTab(AppTab.WRITE)
                                },
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsTab(
                                settings = settings,
                                syncState = syncState,
                                onSignInWithGoogle = viewModel::onSignInWithGoogle,
                                onSignOut = viewModel::onSignOut,
                                onSyncNow = {
                                    viewModel.onSyncNow {
                                        showTransientMessage(syncDoneMsg)
                                    }
                                },
                                onSetCloudSyncEnabled = viewModel::onSetCloudSyncEnabled,
                                onSelectPadBackground = viewModel::onSelectPadBackground,
                                onSelectPenColor = viewModel::onSelectPenColor,
                                onSetShowGuideLines = viewModel::onSetShowGuideLines,
                                onSetAutoConvertEnabled = viewModel::onSetAutoConvertEnabled,
                                onSetWritingPadSize = viewModel::onSetWritingPadSize,
                                onSetCompactLists = viewModel::onSetCompactLists,
                            )
                        }
                    }
                }
            }
        }
    }

    editingTask?.let { task ->
        EditTaskDialog(
            initialText = task.text,
            onDismiss = viewModel::onDismissEditTask,
            onSave = viewModel::onSaveEditTask,
        )
    }
}

@Composable
private fun PInkBottomBar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    NavigationBar(
        containerColor = SlateSurface,
        contentColor = IceWhite,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .testTag("bottom_nav_bar"),
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.WRITE,
            onClick = { onSelectTab(AppTab.WRITE) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Draw,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_write), maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_write"),
        )
        NavigationBarItem(
            selected = currentTab == AppTab.NOTES,
            onClick = { onSelectTab(AppTab.NOTES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.NOTES) {
                        Icons.Rounded.Description
                    } else {
                        Icons.Outlined.Description
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_notes), maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_notes"),
        )
        NavigationBarItem(
            selected = currentTab == AppTab.SETTINGS,
            onClick = { onSelectTab(AppTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.SETTINGS) {
                        Icons.Rounded.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_settings), maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_settings"),
        )
    }
}

@Composable
private fun PInkNavigationRail(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    NavigationRail(
        containerColor = SlateSurface,
        contentColor = IceWhite,
        modifier = Modifier.testTag("nav_rail"),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        NavigationRailItem(
            selected = currentTab == AppTab.WRITE,
            onClick = { onSelectTab(AppTab.WRITE) },
            icon = { Icon(Icons.Rounded.Draw, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_write)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_write"),
        )
        NavigationRailItem(
            selected = currentTab == AppTab.NOTES,
            onClick = { onSelectTab(AppTab.NOTES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.NOTES) {
                        Icons.Rounded.Description
                    } else {
                        Icons.Outlined.Description
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_notes)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_notes"),
        )
        NavigationRailItem(
            selected = currentTab == AppTab.SETTINGS,
            onClick = { onSelectTab(AppTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.SETTINGS) {
                        Icons.Rounded.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_settings)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                indicatorColor = CyanContainer,
                unselectedIconColor = CoolSlate,
                unselectedTextColor = CoolSlate,
            ),
            modifier = Modifier.testTag("nav_tab_settings"),
        )
    }
}

@Composable
private fun WriteAndTasksTab(
    isWideLayout: Boolean,
    portraitPadHeight: Dp,
    strokes: List<InkStroke>,
    draftText: String,
    conversionState: ConversionState,
    tasks: List<TodoTask>,
    settings: AppSettings,
    isPaletteExpanded: Boolean,
    onTogglePalette: () -> Unit,
    onSelectPadBackground: (String) -> Unit,
    onSelectPenColor: (String) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    onEraseStrokes: (Set<Int>) -> Unit,
    onUndoStroke: () -> Unit,
    onClearCanvas: () -> Unit,
    onConvertNow: () -> Unit,
    onDismissError: () -> Unit,
    onDraftTextChanged: (String) -> Unit,
    onClearDraftText: () -> Unit,
    onCopyDraftText: () -> Unit,
    onSaveDraftAsNote: () -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (TodoTask) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (TodoTask) -> Unit,
) {
    if (isWideLayout) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
            ) {
                PInkHeader(
                    tasks = tasks,
                    conversionState = conversionState,
                    settings = settings,
                    isPaletteExpanded = isPaletteExpanded,
                    onTogglePalette = onTogglePalette,
                )
                AnimatedVisibility(
                    visible = isPaletteExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    CompactInkPalettePanel(
                        settings = settings,
                        onSelectBackground = onSelectPadBackground,
                        onSelectPenColor = onSelectPenColor,
                        onClose = onTogglePalette,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                WritingAndComposePane(
                    strokes = strokes,
                    draftText = draftText,
                    conversionState = conversionState,
                    settings = settings,
                    padHeightWeight = 1f,
                    portraitPadHeight = portraitPadHeight,
                    onCanvasSizeChanged = onCanvasSizeChanged,
                    onStrokeStarted = onStrokeStarted,
                    onStrokeCompleted = onStrokeCompleted,
                    onEraseStrokes = onEraseStrokes,
                    onUndoStroke = onUndoStroke,
                    onClearCanvas = onClearCanvas,
                    onConvertNow = onConvertNow,
                    onDismissError = onDismissError,
                    onDraftTextChanged = onDraftTextChanged,
                    onClearDraftText = onClearDraftText,
                    onCopyDraftText = onCopyDraftText,
                    onSaveDraftAsNote = onSaveDraftAsNote,
                    onAddTask = onAddTask,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                TaskListPane(
                    tasks = tasks,
                    compactLists = settings.compactLists,
                    onToggleTask = onToggleTask,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
        ) {
            PInkHeader(
                tasks = tasks,
                conversionState = conversionState,
                settings = settings,
                isPaletteExpanded = isPaletteExpanded,
                onTogglePalette = onTogglePalette,
            )
            AnimatedVisibility(
                visible = isPaletteExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                CompactInkPalettePanel(
                    settings = settings,
                    onSelectBackground = onSelectPadBackground,
                    onSelectPenColor = onSelectPenColor,
                    onClose = onTogglePalette,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            WritingAndComposePane(
                strokes = strokes,
                draftText = draftText,
                conversionState = conversionState,
                settings = settings,
                padHeightWeight = null,
                portraitPadHeight = portraitPadHeight,
                onCanvasSizeChanged = onCanvasSizeChanged,
                onStrokeStarted = onStrokeStarted,
                onStrokeCompleted = onStrokeCompleted,
                onEraseStrokes = onEraseStrokes,
                onUndoStroke = onUndoStroke,
                onClearCanvas = onClearCanvas,
                onConvertNow = onConvertNow,
                onDismissError = onDismissError,
                onDraftTextChanged = onDraftTextChanged,
                onClearDraftText = onClearDraftText,
                onCopyDraftText = onCopyDraftText,
                onSaveDraftAsNote = onSaveDraftAsNote,
                onAddTask = onAddTask,
            )

            Spacer(modifier = Modifier.height(10.dp))

            TaskListPane(
                tasks = tasks,
                compactLists = settings.compactLists,
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun PInkHeader(
    tasks: List<TodoTask>,
    conversionState: ConversionState,
    settings: AppSettings,
    isPaletteExpanded: Boolean,
    onTogglePalette: () -> Unit,
) {
    val activeCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.size - activeCount

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CyanPrimary, PinkAccent))
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = IceWhite,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HeaderStatusPill(conversionState = conversionState)

            if (tasks.isNotEmpty()) {
                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(50)),
                ) {
                    Text(
                        text = "$activeCount • $completedCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolSlate,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            // Palette quick toggle showing active background + pen color
            Surface(
                color = if (isPaletteExpanded) CyanContainer else SlateSurface,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isPaletteExpanded) CyanPrimary else BorderSubtle,
                        shape = CircleShape,
                    )
                    .clickable(onClick = onTogglePalette)
                    .testTag("palette_toggle_button"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = stringResource(R.string.palette_toggle_cd),
                        tint = settings.penOption.strokeColor.let { strokeColor ->
                            if (strokeColor == SlateSurface) CyanPrimary else strokeColor
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderStatusPill(
    conversionState: ConversionState,
) {
    when (conversionState) {
        ConversionState.Converting -> {
            Surface(
                color = CyanContainer.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .testTag("converting_indicator"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = CyanPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.status_converting),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanOnContainer,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        ConversionState.AutoConvertPending -> {
            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .border(1.dp, PinkAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .testTag("auto_pending_indicator"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PinkAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.status_auto_convert_pending),
                        style = MaterialTheme.typography.labelSmall,
                        color = IceWhite,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        else -> {
            Surface(
                color = CyanContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .border(1.dp, BorderSubtle, RoundedCornerShape(50))
                    .testTag("header_spen_status"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Draw,
                        contentDescription = null,
                        tint = CyanOnContainer,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.status_spen_ready),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanOnContainer,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.WritingAndComposePane(
    strokes: List<InkStroke>,
    draftText: String,
    conversionState: ConversionState,
    settings: AppSettings,
    padHeightWeight: Float?,
    portraitPadHeight: Dp,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    onEraseStrokes: (Set<Int>) -> Unit,
    onUndoStroke: () -> Unit,
    onClearCanvas: () -> Unit,
    onConvertNow: () -> Unit,
    onDismissError: () -> Unit,
    onDraftTextChanged: (String) -> Unit,
    onClearDraftText: () -> Unit,
    onCopyDraftText: () -> Unit,
    onSaveDraftAsNote: () -> Unit,
    onAddTask: () -> Unit,
) {
    val padModifier = if (padHeightWeight != null) {
        Modifier
            .fillMaxWidth()
            .weight(padHeightWeight)
    } else {
        Modifier
            .fillMaxWidth()
            .height(portraitPadHeight)
    }

    SPenWritingPad(
        strokes = strokes,
        conversionState = conversionState,
        onCanvasSizeChanged = onCanvasSizeChanged,
        onStrokeStarted = onStrokeStarted,
        onStrokeCompleted = onStrokeCompleted,
        onEraseStrokes = onEraseStrokes,
        padBackgroundColor = settings.backgroundOption.backgroundColor,
        guideLineColor = settings.backgroundOption.guideLineColor,
        penColor = settings.penOption.strokeColor,
        showGuideLines = settings.showGuideLines,
        modifier = padModifier,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Tactile writing-pad buttons: Undo, Clear, Convert
    val hasStrokes = strokes.isNotEmpty()
    val isConverting = conversionState is ConversionState.Converting

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onUndoStroke,
            enabled = hasStrokes,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = CharcoalSurfaceVariant,
                contentColor = IceWhite,
                disabledContainerColor = SlateSurface,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(0.9f)
                .height(48.dp)
                .testTag("undo_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.btn_undo),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        }

        FilledTonalButton(
            onClick = onClearCanvas,
            enabled = hasStrokes,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = CharcoalSurfaceVariant,
                contentColor = IceWhite,
                disabledContainerColor = SlateSurface,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(0.95f)
                .height(48.dp)
                .testTag("clear_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.btn_clear),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        }

        Button(
            onClick = onConvertNow,
            enabled = hasStrokes && !isConverting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PinkContainer,
                contentColor = PinkOnContainer,
                disabledContainerColor = SlateSurface,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier
                .weight(1.35f)
                .height(48.dp)
                .testTag("convert_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.btn_convert),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        }
    }

    // Error banner
    AnimatedVisibility(
        visible = conversionState is ConversionState.Error,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val errorState = conversionState as? ConversionState.Error
        val errorText = when {
            !errorState?.customMessage.isNullOrBlank() -> errorState!!.customMessage!!
            errorState?.messageRes != null -> stringResource(errorState.messageRes)
            else -> stringResource(R.string.error_generic)
        }
        val errorIcon = if (errorState?.messageRes == R.string.error_no_internet) {
            Icons.Rounded.WifiOff
        } else {
            Icons.Rounded.ErrorOutline
        }
        Surface(
            color = RoseErrorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .border(1.dp, RoseError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("error_banner"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = errorIcon,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoseOnErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                if (hasStrokes) {
                    TextButton(
                        onClick = onConvertNow,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = stringResource(R.string.btn_retry),
                            color = RoseOnErrorContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.btn_dismiss),
                            tint = RoseOnErrorContainer,
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Editable recognized text field
    val draftDirection = TextDirectionUtil.textDirectionFor(draftText)
    val draftAlign = TextDirectionUtil.textAlignFor(draftText)

    OutlinedTextField(
        value = draftText,
        onValueChange = onDraftTextChanged,
        placeholder = {
            Text(
                text = stringResource(R.string.draft_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedSlate,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = IceWhite,
            textDirection = draftDirection,
            textAlign = draftAlign,
        ),
        singleLine = false,
        maxLines = 3,
        trailingIcon = {
            if (draftText.isNotEmpty()) {
                IconButton(
                    onClick = onClearDraftText,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.draft_clear_cd),
                        tint = CoolSlate,
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SlateSurface,
            unfocusedContainerColor = SlateSurface,
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = BorderSubtle,
            cursorColor = CyanPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_text_field"),
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Draft actions: Copy all text, Save Note, Add Task
    val hasDraftText = draftText.isNotBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onCopyDraftText,
            enabled = hasDraftText,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = IceWhite,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(0.85f)
                .height(48.dp)
                .testTag("copy_text_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.btn_copy),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        }

        FilledTonalButton(
            onClick = onSaveDraftAsNote,
            enabled = hasDraftText,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = CharcoalSurfaceVariant,
                contentColor = IceWhite,
                disabledContainerColor = SlateSurface,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1.05f)
                .height(48.dp)
                .testTag("save_note_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NoteAdd,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.btn_save_note),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
            )
        }

        Button(
            onClick = onAddTask,
            enabled = hasDraftText,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = CyanOnPrimary,
                disabledContainerColor = CharcoalSurfaceVariant,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier
                .weight(1.1f)
                .height(48.dp)
                .testTag("add_task_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.btn_add),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun TaskListPane(
    tasks: List<TodoTask>,
    compactLists: Boolean,
    onToggleTask: (TodoTask) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (TodoTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (tasks.isEmpty()) {
            EmptyTasksState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("task_list"),
                verticalArrangement = Arrangement.spacedBy(if (compactLists) 6.dp else 10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    SwipeableTodoCard(
                        task = task,
                        compact = compactLists,
                        onToggle = { onToggleTask(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTodoCard(
    task: TodoTask,
    compact: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val isDeleting = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val bgColor by animateColorAsState(
                targetValue = if (isDeleting) RoseErrorContainer else ObsidianBg,
                label = "swipeBg",
            )
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (isDeleting) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.task_delete_cd),
                        tint = RoseError,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
    ) {
        val textDirection = TextDirectionUtil.textDirectionFor(task.text)
        val textAlign = TextDirectionUtil.textAlignFor(task.text)

        Surface(
            color = if (task.isCompleted) SlateSurface.copy(alpha = 0.65f) else SlateSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (task.isCompleted) BorderSubtle.copy(alpha = 0.5f) else BorderSubtle,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onToggle)
                .testTag("task_card_${task.id}"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = if (compact) 2.dp else 6.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("task_toggle_${task.id}"),
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = stringResource(R.string.task_toggle_cd),
                        tint = if (task.isCompleted) EmeraldDone else CyanPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = textDirection,
                        textAlign = textAlign,
                        textDecoration = if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    ),
                    color = if (task.isCompleted) MutedSlate else IceWhite,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEdit)
                        .padding(
                            horizontal = 10.dp,
                            vertical = if (compact) 8.dp else 14.dp,
                        )
                        .testTag("task_text_${task.id}"),
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("task_edit_${task.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.task_edit_title),
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_inkpad),
            contentDescription = stringResource(R.string.empty_state_image_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .widthIn(max = 190.dp)
                .height(108.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleMedium,
            color = IceWhite,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_state_body),
            style = MaterialTheme.typography.bodyMedium,
            color = CoolSlate,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EditTaskDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var editedText by remember(initialText) { mutableStateOf(initialText) }
    val textDirection = TextDirectionUtil.textDirectionFor(editedText)
    val textAlign = TextDirectionUtil.textAlignFor(editedText)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        titleContentColor = IceWhite,
        title = {
            Text(
                text = stringResource(R.string.task_edit_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.task_edit_placeholder),
                        color = MutedSlate,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = IceWhite,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianBg,
                    unfocusedContainerColor = ObsidianBg,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = CyanPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_task_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedText) },
                enabled = editedText.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = CyanOnPrimary,
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("edit_task_save"),
            ) {
                Text(
                    text = stringResource(R.string.btn_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CharcoalSurfaceVariant,
                    contentColor = IceWhite,
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("edit_task_cancel"),
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
