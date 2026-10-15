package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NoteAttachment
import com.example.ui.VaultViewModel
import com.example.ui.components.MarkdownViewer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val title by viewModel.noteTitle.collectAsState()
    val content by viewModel.noteContent.collectAsState()
    val folder by viewModel.noteFolder.collectAsState()
    val tags by viewModel.noteTags.collectAsState()
    val isPinned by viewModel.noteIsPinned.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val attachments by viewModel.noteAttachments.collectAsState()

    val isSaving by viewModel.isSaving.collectAsState()
    val lastSavedTime by viewModel.lastSavedTime.collectAsState()

    var textFieldValue by remember { mutableStateOf(TextFieldValue(content, TextRange(content.length))) }

    // Keep textFieldValue updated when note content changes externally
    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            val cursor = textFieldValue.selection.start.coerceIn(0, content.length)
            textFieldValue = TextFieldValue(content, TextRange(cursor))
        }
    }

    var isDeletedLocally by remember { mutableStateOf(false) }

    // Fast debounced automatic save (400ms)
    LaunchedEffect(title, content) {
        if (!isDeletedLocally && (title.isNotBlank() || content.isNotBlank())) {
            delay(400)
            if (!isDeletedLocally) {
                viewModel.saveCurrentNote()
            }
        }
    }

    // Lifecycle-aware auto-save when leaving or backgrounding the app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (!isDeletedLocally && (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)) {
                viewModel.saveCurrentNote()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (!isDeletedLocally) {
                viewModel.saveCurrentNote()
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }
    var newFolderInput by remember { mutableStateOf(folder) }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }

    val isCursorInBold = remember(textFieldValue, isBoldActive) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        val isAtEmpty = cursor >= 2 && cursor + 2 <= text.length &&
                text.substring(cursor - 2, cursor) == "**" &&
                text.substring(cursor, cursor + 2) == "**"
        val isAtClosing = cursor + 2 <= text.length && text.substring(cursor, cursor + 2) == "**"
        isBoldActive || isAtEmpty || isAtClosing
    }

    val isCursorInItalic = remember(textFieldValue, isItalicActive) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        val isAtEmpty = cursor >= 1 && cursor + 1 <= text.length &&
                text.substring(cursor - 1, cursor) == "*" &&
                text.substring(cursor, cursor + 1) == "*"
        val isAtClosing = cursor + 1 <= text.length && text.substring(cursor, cursor + 1) == "*" &&
                (cursor + 2 > text.length || text.substring(cursor, cursor + 2) != "**")
        isItalicActive || isAtEmpty || isAtClosing
    }

    fun toggleBold() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        if (!selection.collapsed) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selected = text.substring(start, end)
            val newText: String
            val newSelection: TextRange
            if (selected.startsWith("**") && selected.endsWith("**") && selected.length >= 4) {
                val unwrapped = selected.substring(2, selected.length - 2)
                newText = text.substring(0, start) + unwrapped + text.substring(end)
                newSelection = TextRange(start, start + unwrapped.length)
            } else {
                newText = text.substring(0, start) + "**" + selected + "**" + text.substring(end)
                newSelection = TextRange(start + 2, end + 2)
            }
            textFieldValue = TextFieldValue(newText, newSelection)
            viewModel.updateNoteContent(newText)
            isBoldActive = false
        } else {
            val cursor = selection.start
            val isAtEmpty = cursor >= 2 && cursor + 2 <= text.length &&
                    text.substring(cursor - 2, cursor) == "**" &&
                    text.substring(cursor, cursor + 2) == "**"
            val isAtClosing = cursor + 2 <= text.length && text.substring(cursor, cursor + 2) == "**"

            if (isBoldActive || isAtEmpty || isAtClosing) {
                isBoldActive = false
                if (isAtEmpty) {
                    val newText = text.substring(0, cursor - 2) + text.substring(cursor + 2)
                    textFieldValue = TextFieldValue(newText, TextRange(cursor - 2))
                    viewModel.updateNoteContent(newText)
                } else if (isAtClosing) {
                    textFieldValue = TextFieldValue(text, TextRange(cursor + 2))
                }
            } else {
                isBoldActive = true
                val newText = text.substring(0, cursor) + "****" + text.substring(cursor)
                val newCursor = cursor + 2
                textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                viewModel.updateNoteContent(newText)
            }
        }
    }

    fun toggleItalic() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        if (!selection.collapsed) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selected = text.substring(start, end)
            val newText: String
            val newSelection: TextRange
            if (selected.startsWith("*") && selected.endsWith("*") && selected.length >= 2 && !selected.startsWith("**")) {
                val unwrapped = selected.substring(1, selected.length - 1)
                newText = text.substring(0, start) + unwrapped + text.substring(end)
                newSelection = TextRange(start, start + unwrapped.length)
            } else {
                newText = text.substring(0, start) + "*" + selected + "*" + text.substring(end)
                newSelection = TextRange(start + 1, end + 1)
            }
            textFieldValue = TextFieldValue(newText, newSelection)
            viewModel.updateNoteContent(newText)
            isItalicActive = false
        } else {
            val cursor = selection.start
            val isAtEmpty = cursor >= 1 && cursor + 1 <= text.length &&
                    text.substring(cursor - 1, cursor) == "*" &&
                    text.substring(cursor, cursor + 1) == "*"
            val isAtClosing = cursor + 1 <= text.length && text.substring(cursor, cursor + 1) == "*" &&
                    (cursor + 2 > text.length || text.substring(cursor, cursor + 2) != "**")

            if (isItalicActive || isAtEmpty || isAtClosing) {
                isItalicActive = false
                if (isAtEmpty) {
                    val newText = text.substring(0, cursor - 1) + text.substring(cursor + 1)
                    textFieldValue = TextFieldValue(newText, TextRange(cursor - 1))
                    viewModel.updateNoteContent(newText)
                } else if (isAtClosing) {
                    textFieldValue = TextFieldValue(text, TextRange(cursor + 1))
                }
            } else {
                isItalicActive = true
                val newText = text.substring(0, cursor) + "**" + text.substring(cursor)
                val newCursor = cursor + 1
                textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                viewModel.updateNoteContent(newText)
            }
        }
    }

    fun cycleHeading() {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, cursorShift) = when {
            currentLine.startsWith("### ") -> Pair(currentLine.removePrefix("### "), -4)
            currentLine.startsWith("## ") -> Pair("### " + currentLine.removePrefix("## "), 1)
            currentLine.startsWith("# ") -> Pair("## " + currentLine.removePrefix("# "), 1)
            else -> Pair("# $currentLine", 2)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + cursorShift).coerceIn(lineStart, lineStart + newLine.length)
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateNoteContent(newText)
    }

    fun toggleLinePrefix(linePrefix: String) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val newText: String
        val newCursor: Int
        if (currentLine.startsWith(linePrefix)) {
            newText = text.substring(0, lineStart) + currentLine.removePrefix(linePrefix) + text.substring(lineEnd)
            newCursor = (cursor - linePrefix.length).coerceAtLeast(lineStart)
        } else {
            // Strip other common prefixes
            var cleaned = currentLine
            listOf("- [ ] ", "- [x] ", "- ", "1. ", "> ").forEach { p ->
                if (cleaned.startsWith(p)) cleaned = cleaned.removePrefix(p)
            }
            val diff = linePrefix.length - (currentLine.length - cleaned.length)
            newText = text.substring(0, lineStart) + linePrefix + cleaned + text.substring(lineEnd)
            newCursor = (cursor + diff).coerceIn(lineStart, newText.length)
        }
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateNoteContent(newText)
    }

    fun toggleCode() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        if (!selection.collapsed) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            val selected = text.substring(start, end)
            val newText: String
            val newSelection: TextRange
            if (selected.startsWith("`") && selected.endsWith("`") && selected.length >= 2) {
                val unwrapped = selected.substring(1, selected.length - 1)
                newText = text.substring(0, start) + unwrapped + text.substring(end)
                newSelection = TextRange(start, start + unwrapped.length)
            } else {
                newText = text.substring(0, start) + "`" + selected + "`" + text.substring(end)
                newSelection = TextRange(start + 1, end + 1)
            }
            textFieldValue = TextFieldValue(newText, newSelection)
            viewModel.updateNoteContent(newText)
        } else {
            val cursor = selection.start
            val newText = text.substring(0, cursor) + "``" + text.substring(cursor)
            val newCursor = cursor + 1
            textFieldValue = TextFieldValue(newText, TextRange(newCursor))
            viewModel.updateNoteContent(newText)
        }
    }

    fun insertDivider() {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        val prefix = if (cursor > 0 && !text.substring(0, cursor).endsWith("\n")) "\n\n" else "\n"
        val toInsert = "${prefix}---\n\n"
        val newText = text.substring(0, cursor) + toInsert + text.substring(cursor)
        val newCursor = cursor + toInsert.length
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.updateNoteContent(newText)
    }

    // Live Visual Markdown Transformation for instant rich rendering inside the editor
    val markdownVisualTransformation = remember {
        VisualTransformation { annotText ->
            val raw = annotText.text
            val builder = AnnotatedString.Builder(raw)
            val syntaxFaintColor = Color(0x667A756D)
            val codeBgColor = Color(0x1F000000)

            // 1. Bold spans: **text**
            val boldRegex = Regex("""\*\*(.+?)\*\*""")
            boldRegex.findAll(raw).forEach { match ->
                val range = match.range
                builder.addStyle(
                    SpanStyle(color = syntaxFaintColor, fontSize = 11.sp),
                    range.first,
                    range.first + 2
                )
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    range.first + 2,
                    range.last - 1
                )
                builder.addStyle(
                    SpanStyle(color = syntaxFaintColor, fontSize = 11.sp),
                    range.last - 1,
                    range.last + 1
                )
            }

            // 2. Italic spans: *text* (excluding **)
            val italicRegex = Regex("""(?<!\*)\*([^*]+?)\*(?!\*)""")
            italicRegex.findAll(raw).forEach { match ->
                val range = match.range
                builder.addStyle(
                    SpanStyle(color = syntaxFaintColor, fontSize = 11.sp),
                    range.first,
                    range.first + 1
                )
                builder.addStyle(
                    SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    range.first + 1,
                    range.last
                )
                builder.addStyle(
                    SpanStyle(color = syntaxFaintColor, fontSize = 11.sp),
                    range.last,
                    range.last + 1
                )
            }

            // 3. Headings: # Heading
            val headingRegex = Regex("""(?m)^(#{1,3})\s+(.*)$""")
            headingRegex.findAll(raw).forEach { match ->
                val hashes = match.groups[1]!!
                val textGroup = match.groups[2]!!
                builder.addStyle(
                    SpanStyle(color = syntaxFaintColor, fontSize = 11.sp),
                    hashes.range.first,
                    hashes.range.last + 1
                )
                val fontSize = when (hashes.value.length) {
                    1 -> 20.sp
                    2 -> 18.sp
                    else -> 16.sp
                }
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize),
                    textGroup.range.first,
                    textGroup.range.last + 1
                )
            }

            // 4. Inline code: `code`
            val codeRegex = Regex("""`([^`\n]+)`""")
            codeRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    // Android Photo Picker for zero-permission image attachments
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "foto_${System.currentTimeMillis()}.jpg"
            viewModel.addAttachmentFromUri(uri, fileName)
        }
    }

    // Android Document Picker for attachments
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "adjunto_${System.currentTimeMillis()}"
            viewModel.addAttachmentFromUri(uri, fileName)
        }
    }

    // BackHandler to auto-save note and return to list
    BackHandler {
        if (!isDeletedLocally) {
            viewModel.saveCurrentNote()
        }
        viewModel.navigateTo(VaultViewModel.Screen.VaultList)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AES-256",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldLight
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isSaving) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = com.example.ui.theme.FolderAmber
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Guardando...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = com.example.ui.theme.TextSecondaryMuted
                                )
                            }
                        } else if (lastSavedTime != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.EmeraldSecurity,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Guardado",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = com.example.ui.theme.EmeraldSecurity
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isDeletedLocally) {
                                viewModel.saveCurrentNote()
                            }
                            viewModel.navigateTo(VaultViewModel.Screen.VaultList)
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a la bóveda"
                        )
                    }
                },
                actions = {
                    // Preview vs Edit Mode Toggle
                    IconButton(
                        onClick = { viewModel.togglePreviewMode() },
                        modifier = Modifier.testTag("toggle_preview_button")
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "Modo Edición" else "Vista Previa Markdown",
                            tint = if (isPreviewMode) CyanAccent else EmeraldPrimary
                        )
                    }

                    // Export as Markdown
                    IconButton(
                        onClick = { viewModel.exportCurrentNoteAsMarkdown(context) },
                        modifier = Modifier.testTag("export_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportar Markdown (.md)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle Pin
                    IconButton(
                        onClick = { viewModel.togglePinned() },
                        modifier = Modifier.testTag("pin_note_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Fijar nota",
                            tint = if (isPinned) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete Note
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar nota",
                            tint = RoseError.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Note Meta Section (Folder & Tags)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Folder Chip (clickable to change)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            newFolderInput = folder
                            showFolderDialog = true
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📁 $folder",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = EmeraldLight
                        )
                    }
                }

                // Add Attachment Shortcut
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Adjuntar Foto",
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar Archivo",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Tags Row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF083344)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar tag",
                                tint = CyanAccent.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { viewModel.removeTag(tag) }
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showAddTagDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Etiqueta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Title Field
            BasicTextField(
                value = title,
                onValueChange = { viewModel.updateNoteTitle(it) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(EmeraldPrimary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Título de la nota...",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("note_title_input")
            )

            // Markdown Formatting Toolbar (Shown only in Edit Mode)
            if (!isPreviewMode) {
                MarkdownToolbar(
                    isBoldActive = isCursorInBold,
                    isItalicActive = isCursorInItalic,
                    onToggleBold = { toggleBold() },
                    onToggleItalic = { toggleItalic() },
                    onToggleHeading = { cycleHeading() },
                    onToggleChecklist = { toggleLinePrefix("- [ ] ") },
                    onToggleBulletList = { toggleLinePrefix("- ") },
                    onToggleNumberedList = { toggleLinePrefix("1. ") },
                    onToggleQuote = { toggleLinePrefix("> ") },
                    onToggleCode = { toggleCode() },
                    onInsertDivider = { insertDivider() }
                )
            }

            // Main Editor or Markdown Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (isPreviewMode) {
                    // Markdown Preview
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        MarkdownViewer(
                            markdownText = content,
                            onCheckboxToggle = { lineIdx, isChecked ->
                                viewModel.toggleChecklistLine(lineIdx, isChecked)
                            }
                        )

                        // Attachments Gallery in Preview
                        if (attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            AttachmentsSection(
                                attachments = attachments,
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    // Markdown Text Editor
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                viewModel.updateNoteContent(it.text)
                            },
                            visualTransformation = markdownVisualTransformation,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(EmeraldPrimary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 120.dp)
                                ) {
                                    if (content.isEmpty()) {
                                        Text(
                                            text = "Comienza a redactar en Markdown...\n\n# Encabezado\n- [ ] Tarea pendiente\n> Cita inspiradora\n**Texto en negrita**",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                lineHeight = 24.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_content_input")
                        )

                        if (attachments.isNotEmpty()) {
                            AttachmentsSection(
                                attachments = attachments,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Añadir Etiqueta") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("ej. finanzas, ideas, notas") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagInput.isNotBlank()) {
                            viewModel.addTag(newTagInput)
                            newTagInput = ""
                        }
                        showAddTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Añadir", color = Color(0xFF022C22))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Change Folder Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Mover a Carpeta") },
            text = {
                OutlinedTextField(
                    value = newFolderInput,
                    onValueChange = { newFolderInput = it },
                    placeholder = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderInput.isNotBlank()) {
                            viewModel.updateNoteFolder(newFolderInput)
                        }
                        showFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar", color = Color(0xFF022C22))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Note Confirm Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar nota?") },
            text = { Text("Esta nota cifrada y sus archivos adjuntos se borrarán de forma irreversible de tu bóveda.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletedLocally = true
                        showDeleteConfirm = false
                        viewModel.deleteCurrentNote()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MarkdownToolbar(
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleHeading: () -> Unit,
    onToggleChecklist: () -> Unit,
    onToggleBulletList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onToggleQuote: () -> Unit,
    onToggleCode: () -> Unit,
    onInsertDivider: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Negrita (Bold)
            ToolbarIconButton(
                icon = Icons.Default.FormatBold,
                label = "Negrita",
                isActive = isBoldActive,
                onClick = onToggleBold
            )

            // 2. Cursiva (Italic)
            ToolbarIconButton(
                icon = Icons.Default.FormatItalic,
                label = "Cursiva",
                isActive = isItalicActive,
                onClick = onToggleItalic
            )

            // 3. Encabezado / Título (H)
            ToolbarIconButton(
                icon = Icons.Default.Title,
                label = "Encabezado (H1/H2/H3)",
                onClick = onToggleHeading
            )

            // 4. Lista de tareas (Checklist)
            ToolbarIconButton(
                icon = Icons.Default.CheckBox,
                label = "Lista de tareas",
                onClick = onToggleChecklist
            )

            // 5. Lista con viñetas
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = "Lista con viñetas",
                onClick = onToggleBulletList
            )

            // 6. Lista numerada
            ToolbarIconButton(
                icon = Icons.Default.FormatListNumbered,
                label = "Lista numerada",
                onClick = onToggleNumberedList
            )

            // 7. Cita
            ToolbarIconButton(
                icon = Icons.Default.FormatQuote,
                label = "Cita",
                onClick = onToggleQuote
            )

            // 8. Código
            ToolbarIconButton(
                icon = Icons.Default.Code,
                label = "Código",
                onClick = onToggleCode
            )

            // 9. Línea divisoria
            ToolbarIconButton(
                icon = Icons.Default.HorizontalRule,
                label = "Separador",
                onClick = onInsertDivider
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) com.example.ui.theme.TextPrimaryDark else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) com.example.ui.theme.EmeraldPrimary else com.example.ui.theme.BorderSubtleWarm
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) com.example.ui.theme.EmeraldLight else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AttachmentsSection(
    attachments: List<NoteAttachment>,
    viewModel: VaultViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Archivos Adjuntos Cifrados (${attachments.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        attachments.forEach { att ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF064E3B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (att.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = att.fileName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Cifrado AES-256 • ${att.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.deleteAttachment(att.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar adjunto",
                            tint = RoseError.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
