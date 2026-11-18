package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.NoteEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanOnContainer
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkOnContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseErrorContainer
import com.example.ui.theme.SlateSurface
import com.example.util.TextDirectionUtil
import java.text.DateFormat
import java.util.Date

@Composable
fun NotesTab(
    notes: List<NoteEntity>,
    searchQuery: String,
    compactLists: Boolean,
    editingNote: NoteEntity?,
    onSearchQueryChanged: (String) -> Unit,
    onCreateNewNote: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onCopyNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onDismissNoteDialog: () -> Unit,
    onSaveNoteDialog: (title: String, content: String) -> Unit,
    onLoadNoteToWrite: (NoteEntity) -> Unit,
    onAddNoteAsTask: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredNotes = remember(notes, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(q, ignoreCase = true) ||
                    note.content.contains(q, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.notes_header),
                    style = MaterialTheme.typography.headlineMedium,
                    color = IceWhite,
                )
                if (notes.isNotEmpty()) {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(50)),
                    ) {
                        Text(
                            text = "${notes.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolSlate,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = stringResource(R.string.notes_search_placeholder),
                        color = MutedSlate,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = CoolSlate,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface,
                    unfocusedContainerColor = SlateSurface,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = IceWhite,
                    unfocusedTextColor = IceWhite,
                    cursorColor = CyanPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_field"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CyanContainer.copy(alpha = 0.45f))
                            .border(1.dp, BorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = CyanOnContainer,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (notes.isEmpty()) {
                            stringResource(R.string.notes_empty_title)
                        } else {
                            stringResource(R.string.notes_empty_search)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite,
                        textAlign = TextAlign.Center,
                    )
                    if (notes.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.notes_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_list"),
                    verticalArrangement = Arrangement.spacedBy(if (compactLists) 8.dp else 12.dp),
                    contentPadding = PaddingValues(bottom = 84.dp),
                ) {
                    items(
                        items = filteredNotes,
                        key = { it.id },
                    ) { note ->
                        SwipeableNoteCard(
                            note = note,
                            compact = compactLists,
                            onOpen = { onOpenNote(note) },
                            onCopy = { onCopyNote(note) },
                            onDelete = { onDeleteNote(note) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateNewNote,
            containerColor = PinkContainer,
            contentColor = PinkOnContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 4.dp)
                .testTag("create_note_fab"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NoteAdd,
                contentDescription = stringResource(R.string.note_fab_cd),
            )
        }
    }

    editingNote?.let { note ->
        NoteDetailDialog(
            note = note,
            onDismiss = onDismissNoteDialog,
            onSave = onSaveNoteDialog,
            onCopy = onCopyNote,
            onLoadToWrite = {
                onDismissNoteDialog()
                onLoadNoteToWrite(note)
            },
            onAddAsTask = {
                onDismissNoteDialog()
                onAddNoteAsTask(note)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNoteCard(
    note: NoteEntity,
    compact: Boolean,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
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
                label = "noteSwipeBg",
            )
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (isDeleting) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.note_delete_cd),
                        tint = RoseError,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
    ) {
        val displayTitle = note.title.ifBlank { stringResource(R.string.note_untitled) }
        val titleDirection = TextDirectionUtil.textDirectionFor(displayTitle)
        val bodyDirection = TextDirectionUtil.textDirectionFor(note.content)
        val formattedTime = remember(note.updatedAt) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(note.updatedAt))
        }

        Surface(
            color = SlateSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .clickable(onClick = onOpen)
                .testTag("note_card_${note.id}"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (compact) 10.dp else 14.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDirection = titleDirection,
                        ),
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("note_copy_${note.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.note_copy_cd),
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("note_delete_${note.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.note_delete_cd),
                            tint = MutedSlate,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = bodyDirection,
                    ),
                    color = CoolSlate,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
                )
            }
        }
    }
}

@Composable
private fun NoteDetailDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit,
    onCopy: (NoteEntity) -> Unit,
    onLoadToWrite: () -> Unit,
    onAddAsTask: () -> Unit,
) {
    val isNew = note.id <= 0L
    var titleText by remember(note.id, note.title) { mutableStateOf(note.title) }
    var contentText by remember(note.id, note.content) { mutableStateOf(note.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        titleContentColor = IceWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (isNew) R.string.note_new_title else R.string.note_edit_title
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (contentText.isNotBlank()) {
                    IconButton(
                        onClick = { onCopy(note.copy(title = titleText, content = contentText)) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.note_copy_cd),
                            tint = CyanPrimary,
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.note_title_placeholder),
                            color = MutedSlate,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = IceWhite,
                        textDirection = TextDirectionUtil.textDirectionFor(titleText),
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = CyanPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_dialog_title"),
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.note_content_placeholder),
                            color = MutedSlate,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = IceWhite,
                        textDirection = TextDirectionUtil.textDirectionFor(contentText),
                    ),
                    singleLine = false,
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = CyanPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_dialog_content"),
                )

                if (!isNew && contentText.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onLoadToWrite,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("note_send_to_write_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Draw,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.note_load_to_write),
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        OutlinedButton(
                            onClick = onAddAsTask,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("note_to_task_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.note_add_task_from_note),
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(titleText, contentText) },
                enabled = contentText.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = CyanOnPrimary,
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("note_dialog_save"),
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
                    .testTag("note_dialog_cancel"),
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
