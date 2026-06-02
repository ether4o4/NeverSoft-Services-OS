package com.ether4o4.morsvitaest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.Project
import com.ether4o4.morsvitaest.data.ProjectDocument
import com.ether4o4.morsvitaest.ui.handCursor

/**
 * Projects tab content. The Project is the app's persistent context container:
 * its instructions get prepended to the system prompt on every send regardless
 * of which AI service is active, so the same context follows the user across
 * model swaps.
 */
@Composable
internal fun ProjectsContent(uiState: SettingsUiState, actions: SettingsActions) {
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Project?>(null) }
    var confirmDelete by remember { mutableStateOf<Project?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsCard {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A project is a persistent context container. Its instructions get prepended to every chat regardless of which AI service is active — so the same framing follows you across model swaps. Files and per-project memory come in later phases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // "None" entry — turn off the active project without deleting anything.
        ProjectRow(
            displayName = "(no active project)",
            instructionsPreview = "Chat uses your global Agent settings only.",
            isActive = uiState.activeProjectId == Project.NONE_ID,
            onClick = { actions.onSelectActiveProject(Project.NONE_ID) },
            onEdit = null,
            onDelete = null,
        )

        uiState.projects.forEach { project ->
            val docCount = project.documents.count { it.content.isNotBlank() }
            val preview = buildString {
                if (project.instructions.isBlank()) {
                    append("(no instructions)")
                } else {
                    append(project.instructions)
                }
                if (docCount > 0) {
                    append(" • $docCount doc")
                    if (docCount != 1) append("s")
                }
            }
            ProjectRow(
                displayName = project.name,
                instructionsPreview = preview,
                isActive = uiState.activeProjectId == project.id,
                onClick = { actions.onSelectActiveProject(project.id) },
                onEdit = { editing = project },
                onDelete = { confirmDelete = project },
            )
        }

        Button(
            onClick = { showCreate = true },
            modifier = Modifier.handCursor(),
        ) { Text("New project") }
    }

    if (showCreate) {
        ProjectEditDialog(
            initialName = "",
            initialInstructions = "",
            initialDocuments = emptyList(),
            title = "New project",
            onDismiss = { showCreate = false },
            onConfirm = { name, instructions, documents ->
                actions.onCreateProject(name, instructions, documents)
                showCreate = false
            },
        )
    }

    editing?.let { project ->
        ProjectEditDialog(
            initialName = project.name,
            initialInstructions = project.instructions,
            initialDocuments = project.documents,
            title = "Edit project",
            onDismiss = { editing = null },
            onConfirm = { name, instructions, documents ->
                actions.onUpdateProject(project.id, name, instructions, documents)
                editing = null
            },
        )
    }

    confirmDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete project?") },
            text = { Text("\"${project.name}\" will be deleted. Chat history isn't affected (Phase 1.5 will tie conversations to projects).") },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDeleteProject(project.id)
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProjectRow(
    displayName: String,
    instructionsPreview: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    SettingsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isActive,
                onClick = onClick,
                modifier = Modifier.handCursor(),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = instructionsPreview.take(140) + if (instructionsPreview.length > 140) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (onEdit != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onEdit, modifier = Modifier.handCursor()) {
                    Text("Edit")
                }
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.handCursor()) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ProjectEditDialog(
    initialName: String,
    initialInstructions: String,
    initialDocuments: List<ProjectDocument>,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<ProjectDocument>) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var instructions by remember { mutableStateOf(initialInstructions) }
    val documents = remember { initialDocuments.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (prepended to every chat)") },
                    placeholder = { Text("e.g. \"You are helping me ship MVE. Be terse. Always include file:line references.\"") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Reference text the AI sees on every chat in this project (notes, specs, style guides, code snippets — markdown OK).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                documents.forEachIndexed { idx, doc ->
                    DocumentEditor(
                        doc = doc,
                        onChange = { updated -> documents[idx] = updated },
                        onDelete = { documents.removeAt(idx) },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = {
                        documents.add(
                            ProjectDocument(name = "Document ${documents.size + 1}", content = ""),
                        )
                    },
                    modifier = Modifier.handCursor(),
                ) {
                    Text("+ Add document")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, instructions, documents.toList()) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DocumentEditor(
    doc: ProjectDocument,
    onChange: (ProjectDocument) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = doc.name,
                onValueChange = { onChange(doc.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onDelete, modifier = Modifier.handCursor()) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = doc.content,
            onValueChange = { onChange(doc.copy(content = it)) },
            label = { Text("Content") },
            placeholder = { Text("Paste reference text, notes, code snippets, etc.") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 240.dp),
        )
    }
}
