package com.ether4o4.morsvitaest.ui.retrosSpace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.data.DataRepository
import com.ether4o4.morsvitaest.ui.settings.SettingsUiState
import com.ether4o4.morsvitaest.ui.settings.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val SpaceBg = Color(0xFF020814)
private val Panel = Color(0xFF07182B)
private val Panel2 = Color(0xFF0A2036)
private val Cyan = Color(0xFF35D9FF)
private val Blue = Color(0xFF5C8CFF)
private val Violet = Color(0xFF9A68FF)
private val TextMain = Color(0xFFE8F2FF)
private val TextDim = Color(0xFF8197AF)

private data class SpaceItem(val title: String, val subtitle: String, val icon: ImageVector, val tint: Color, val action: SpaceAction)
private enum class SpaceAction { Models, Projects, Connectors, Knowledge, Skills, Missions, Vibes, Files, Browser, Terminal, Diagnostics, Settings }

@Composable
fun RetroSpaceHome(
    onOpenSettings: (String) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val vm = koinViewModel<SettingsViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val dataRepository = koinInject<DataRepository>()
    var selected by remember { mutableStateOf("Workspace") }
    var showVibe by remember { mutableStateOf(false) }

    val items = listOf(
        SpaceItem("AI MODELS", state.configuredServices.size.toString() + " configured", Icons.Filled.SmartToy, Cyan, SpaceAction.Models),
        SpaceItem("PROJECTS", state.projects.size.toString() + " projects", Icons.Filled.Folder, Blue, SpaceAction.Projects),
        SpaceItem("CONNECTORS", state.mcpServers.size.toString() + " MCP servers", Icons.Filled.Link, Cyan, SpaceAction.Connectors),
        SpaceItem("KNOWLEDGE", state.memories.size.toString() + " memories", Icons.Filled.Psychology, Violet, SpaceAction.Knowledge),
        SpaceItem("SKILLS", state.tools.size.toString() + " tools", Icons.Filled.Build, Blue, SpaceAction.Skills),
        SpaceItem("MISSIONS", state.scheduledTasks.size.toString() + " scheduled", Icons.Filled.Flag, Violet, SpaceAction.Missions),
        SpaceItem("VIBES", "agent profile", Icons.Filled.Tune, Cyan, SpaceAction.Vibes),
        SpaceItem("FILES", "workspace + sandbox", Icons.Filled.Description, Blue, SpaceAction.Files),
        SpaceItem("BROWSER", "research surface", Icons.Filled.Language, Cyan, SpaceAction.Browser),
        SpaceItem("TERMINAL", "Linux sandbox", Icons.Filled.Terminal, Violet, SpaceAction.Terminal),
        SpaceItem("DIAGNOSTICS", "system state", Icons.Filled.BugReport, Blue, SpaceAction.Diagnostics),
        SpaceItem("SETTINGS", "workspace control", Icons.Filled.Settings, TextDim, SpaceAction.Settings),
    )

    fun activate(item: SpaceItem) {
        selected = item.title
        when (item.action) {
            SpaceAction.Models -> onOpenSettings("Services")
            SpaceAction.Projects -> onOpenSettings("Projects")
            SpaceAction.Connectors -> onOpenSettings("Tools")
            SpaceAction.Knowledge -> onOpenSettings("Agent")
            SpaceAction.Skills -> onOpenSettings("Tools")
            SpaceAction.Missions -> onOpenSettings("General")
            SpaceAction.Vibes -> showVibe = true
            SpaceAction.Files -> onOpenFiles()
            SpaceAction.Browser -> onOpenBrowser()
            SpaceAction.Terminal -> onOpenTerminal()
            SpaceAction.Diagnostics -> onOpenSettings("General")
            SpaceAction.Settings -> onOpenSettings("General")
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(SpaceBg).navigationBarsPadding()) {
        val compact = maxWidth < 700.dp
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                SpaceHeader(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), true, state)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) { items.forEach { RailChip(it, selected == it.title) { activate(it) } } }
                SpaceBody(Modifier.weight(1f), state, selected, items, { activate(it) }, true)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(176.dp).fillMaxHeight().padding(12.dp)) {
                    SpaceHeader(Modifier.padding(bottom = 14.dp), false, state)
                    items.forEach { RailItem(it, selected == it.title) { activate(it) } }
                }
                SpaceBody(Modifier.weight(1f), state, selected, items, { activate(it) }, false)
            }
        }
    }

    if (showVibe) {
        AlertDialog(
            onDismissRequest = { showVibe = false },
            title = { Text("SET THE VIBE") },
            text = { Text("Save a focused Retro-Space agent profile: creative, fast, minimal, production-focused, direct, and no over-explaining.") },
            confirmButton = {
                TextButton(onClick = {
                    dataRepository.setSoulText("Retro-Space Vibe: creative, fast, minimal, production-focused. Prefer direct answers, concrete execution, concise explanations, and preserve the user's requested style.")
                    showVibe = false
                }) { Text("APPLY") }
            },
            dismissButton = { TextButton(onClick = { showVibe = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun SpaceHeader(modifier: Modifier, compact: Boolean, state: SettingsUiState) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Hub, null, tint = Cyan, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("RETRO-SPACE", color = TextMain, fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Bold)
                Text("AI WORKSPACE ENVIRONMENT", color = TextDim, fontSize = 8.sp, letterSpacing = 1.4.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(state.configuredServices.size.toString() + " AI SERVICES  •  " + state.projects.size + " PROJECTS", color = Cyan, fontSize = 8.sp)
    }
}

@Composable
private fun SpaceBody(
    modifier: Modifier,
    state: SettingsUiState,
    selected: String,
    items: List<SpaceItem>,
    onSelect: (SpaceItem) -> Unit,
    compact: Boolean,
) {
    Column(modifier.fillMaxSize().padding(horizontal = if (compact) 10.dp else 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(bottom = 10.dp)
        ) {
            Column(Modifier.padding(if (compact) 13.dp else 15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("CONTEXT CORE", color = TextDim, fontSize = 8.sp, letterSpacing = 1.5.sp)
                        Text(
                            if (state.activeProjectId == com.ether4o4.morsvitaest.data.Project.NONE_ID) "Workspace ready" else "Active project loaded",
                            color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(state.memories.size.toString() + " MEM", color = Violet, fontSize = 9.sp)
                }
                Spacer(Modifier.height(10.dp))
                Divider(color = Color(0xFF16324D))
                Spacer(Modifier.height(10.dp))
                Text("MVE ENGINE  •  SERVICES  •  MEMORY  •  MCP  •  SANDBOX", color = TextDim, fontSize = 8.sp)
            }
        }

        val modelCards = state.configuredServices.map { entry ->
            SpaceItem(
                entry.selectedModel?.displayName ?: entry.service.displayName,
                entry.selectedModel?.subtitle ?: "Provider • tap to configure",
                Icons.Filled.SmartToy,
                if (entry.enabled) Cyan else TextDim,
                SpaceAction.Models
            )
        }.take(4)

        val projectCards = state.projects.map { project ->
            SpaceItem(
                project.name,
                if (project.id == state.activeProjectId) "Active project" else "Persistent project",
                Icons.Filled.Folder,
                Blue,
                SpaceAction.Projects
            )
        }.take(4)

        val cards = when {
            modelCards.isNotEmpty() -> modelCards
            projectCards.isNotEmpty() -> projectCards
            else -> listOf(
                SpaceItem("NO AI SERVICES", "Add a provider in Models", Icons.Filled.SmartToy, Cyan, SpaceAction.Models),
                SpaceItem("NO PROJECTS", "Create one in Projects", Icons.Filled.Folder, Blue, SpaceAction.Projects),
                SpaceItem("MEMORY", state.memories.size.toString() + " stored", Icons.Filled.Storage, Violet, SpaceAction.Knowledge),
                SpaceItem("MCP", state.mcpServers.size.toString() + " connected", Icons.Filled.Link, Cyan, SpaceAction.Connectors)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (compact) 132.dp else 170.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).widthIn(max = 760.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { items(cards) { SpaceCard(it, selected == it.title) { onSelect(it) } } }

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel2),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(top = 8.dp)
        ) {
            Column(Modifier.padding(11.dp)) {
                Text(selected, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    when (selected) {
                        "AI MODELS" -> "Real MVE provider and local-model configuration."
                        "PROJECTS" -> "Persistent projects and active project state."
                        "CONNECTORS" -> "MCP and tool configuration from the MVE engine."
                        "KNOWLEDGE" -> "Agent memory and profile controls."
                        "SKILLS" -> "Registered tools available to the agent."
                        "MISSIONS" -> "Scheduled tasks and autonomous controls."
                        "VIBES" -> "Persistent agent profile applied through MVE soul settings."
                        "FILES" -> "Existing sandbox and file system."
                        "TERMINAL" -> "Existing Linux sandbox terminal."
                        "BROWSER" -> "Research surface staged for the next integration."
                        else -> "Open the real MVE control surface."
                    },
                    color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SpaceCard(item: SpaceItem, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF0E3048) else Panel),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(item.title, color = TextMain, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(7.dp))
            Text(item.subtitle, color = TextDim, fontSize = 8.sp)
        }
    }
}

@Composable
private fun RailItem(item: SpaceItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(if (selected) Color(0xFF0D344A) else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Text(item.title, color = if (selected) TextMain else TextDim, fontSize = 9.sp)
    }
}

@Composable
private fun RailChip(item: SpaceItem, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF0D344A) else Panel),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(item.title, color = TextMain, fontSize = 8.sp)
        }
    }
}
