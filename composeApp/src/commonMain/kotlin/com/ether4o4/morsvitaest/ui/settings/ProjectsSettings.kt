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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.Project
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
            ProjectRow(
                displayName = project.name,
                instructionsPreview = project.instructions.ifBlank { "(no instructions)" },
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
            title = "New project",
            onDismiss = { showCreate = false },
            onConfirm = { name, instructions ->
                actions.onCreateProject(name, instructions)
                showCreate = false
            },
        )
    }

    editing?.let { project ->
        ProjectEditDialog(
            initialName = project.name,
            initialInstructions = project.instructions,
            title = "Edit project",
            onDismiss = { editing = null },
            onConfirm = { name, instructions ->
                actions.onUpdateProject(project.id, name, instructions)
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
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var instructions by remember { mutableStateOf(initialInstructions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, instructions) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
