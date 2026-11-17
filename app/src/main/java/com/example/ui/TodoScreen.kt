package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
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
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val draftText by viewModel.draftText.collectAsStateWithLifecycle()
    val conversionState by viewModel.conversionState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.snackbar_task_deleted)
    val undoLabel = stringResource(R.string.snackbar_undo)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val isWideLayout = maxWidth >= 700.dp

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
                        PInkHeader(tasks = tasks)
                        Spacer(modifier = Modifier.height(14.dp))
                        WritingAndComposePane(
                            strokes = strokes,
                            draftText = draftText,
                            conversionState = conversionState,
                            padHeightWeight = 1f,
                            onCanvasSizeChanged = viewModel::onCanvasSizeChanged,
                            onStrokeStarted = viewModel::onStrokeStarted,
                            onStrokeCompleted = viewModel::onStrokeCompleted,
                            onUndoStroke = viewModel::onUndoStroke,
                            onClearCanvas = viewModel::onClearCanvas,
                            onConvertNow = viewModel::onConvertNow,
                            onDismissError = viewModel::onDismissError,
                            onDraftTextChanged = viewModel::onDraftTextChanged,
                            onClearDraftText = viewModel::onClearDraftText,
                            onAddTask = viewModel::onAddTask,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        TaskListPane(
                            tasks = tasks,
                            onToggleTask = viewModel::onToggleTask,
                            onEditTask = viewModel::onStartEditTask,
                            onDeleteTask = { task ->
                                viewModel.onDeleteTask(task)
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = deletedMsg,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.onUndoDelete()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .align(Alignment.TopCenter),
                ) {
                    PInkHeader(tasks = tasks)
                    Spacer(modifier = Modifier.height(12.dp))

                    WritingAndComposePane(
                        strokes = strokes,
                        draftText = draftText,
                        conversionState = conversionState,
                        padHeightWeight = null,
                        onCanvasSizeChanged = viewModel::onCanvasSizeChanged,
                        onStrokeStarted = viewModel::onStrokeStarted,
                        onStrokeCompleted = viewModel::onStrokeCompleted,
                        onUndoStroke = viewModel::onUndoStroke,
                        onClearCanvas = viewModel::onClearCanvas,
                        onConvertNow = viewModel::onConvertNow,
                        onDismissError = viewModel::onDismissError,
                        onDraftTextChanged = viewModel::onDraftTextChanged,
                        onClearDraftText = viewModel::onClearDraftText,
                        onAddTask = viewModel::onAddTask,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TaskListPane(
                        tasks = tasks,
                        onToggleTask = viewModel::onToggleTask,
                        onEditTask = viewModel::onStartEditTask,
                        onDeleteTask = { task ->
                            viewModel.onDeleteTask(task)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = deletedMsg,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.onUndoDelete()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
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
private fun PInkHeader(
    tasks: List<TodoTask>,
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
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = IceWhite,
            )
        }

        if (tasks.isNotEmpty()) {
            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(50)),
            ) {
                Text(
                    text = "$activeCount open • $completedCount done",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.WritingAndComposePane(
    strokes: List<InkStroke>,
    draftText: String,
    conversionState: ConversionState,
    padHeightWeight: Float?,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    onUndoStroke: () -> Unit,
    onClearCanvas: () -> Unit,
    onConvertNow: () -> Unit,
    onDismissError: () -> Unit,
    onDraftTextChanged: (String) -> Unit,
    onClearDraftText: () -> Unit,
    onAddTask: () -> Unit,
) {
    val padModifier = if (padHeightWeight != null) {
        Modifier
            .fillMaxWidth()
            .weight(padHeightWeight)
    } else {
        Modifier
            .fillMaxWidth()
            .height(210.dp)
    }

    SPenWritingPad(
        strokes = strokes,
        conversionState = conversionState,
        onCanvasSizeChanged = onCanvasSizeChanged,
        onStrokeStarted = onStrokeStarted,
        onStrokeCompleted = onStrokeCompleted,
        modifier = padModifier,
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Big tactile writing-pad buttons: Undo, Clear, Convert
    val hasStrokes = strokes.isNotEmpty()
    val isConverting = conversionState is ConversionState.Converting

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("undo_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_undo),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
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
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("clear_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_clear),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
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
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .weight(1.15f)
                .height(52.dp)
                .testTag("convert_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_convert),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }

    // Clear, high-contrast error banner (offline / timeout / recognition issues)
    AnimatedVisibility(
        visible = conversionState is ConversionState.Error,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val errorState = conversionState as? ConversionState.Error
        val errorText = when {
            errorState?.messageRes != null -> stringResource(errorState.messageRes)
            errorState?.customMessage != null -> errorState.customMessage
            else -> stringResource(R.string.error_generic)
        }
        Surface(
            color = RoseErrorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .border(1.dp, RoseError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("error_banner"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(22.dp),
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

    Spacer(modifier = Modifier.height(12.dp))

    // Editable recognized text field + Big Add button
    val draftDirection = TextDirectionUtil.textDirectionFor(draftText)
    val draftAlign = TextDirectionUtil.textAlignFor(draftText)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
                .weight(1f)
                .testTag("draft_text_field"),
        )

        Button(
            onClick = onAddTask,
            enabled = draftText.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = CyanOnPrimary,
                disabledContainerColor = CharcoalSurfaceVariant,
                disabledContentColor = MutedSlate,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier
                .height(56.dp)
                .testTag("add_task_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_add),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TaskListPane(
    tasks: List<TodoTask>,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    SwipeableTodoCard(
                        task = task,
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
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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

                // Tapping the task text opens the editor
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
                        .padding(horizontal = 10.dp, vertical = 14.dp)
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
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_inkpad),
            contentDescription = stringResource(R.string.empty_state_image_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .widthIn(max = 240.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleMedium,
            color = IceWhite,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
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
