package com.ether4o4.morsvitaest.ui.chat.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.data.Project
import com.ether4o4.morsvitaest.ui.handCursor
import org.koin.compose.koinInject

/**
 * Bottom-sheet project switcher reached from the chat top bar. Lists the user's
 * projects (plus a "no project" option); tapping one makes its instructions the
 * active context for every chat. Reads/writes through [DataRepository] so it needs
 * no host wiring. Create / edit / delete still live in Settings → Projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectPickerSheet(onDismiss: () -> Unit) {
    val repo = koinInject<DataRepository>()
    val projects = remember { repo.getProjects() }
    val activeId = remember { repo.getActiveProject()?.id ?: Project.NONE_ID }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap a project to make its instructions the active context for every chat. " +
                    "Create, edit, and delete projects in Settings → Projects.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            ProjectPickRow(
                name = "(No project)",
                preview = "Global Agent settings only.",
                active = activeId == Project.NONE_ID,
            ) {
                repo.setActiveProjectId(Project.NONE_ID)
                onDismiss()
            }
            projects.forEach { project ->
                ProjectPickRow(
                    name = project.name,
                    preview = project.instructions.ifBlank { "(no instructions)" },
                    active = activeId == project.id,
                ) {
                    repo.setActiveProjectId(project.id)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ProjectPickRow(name: String, preview: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .handCursor()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
