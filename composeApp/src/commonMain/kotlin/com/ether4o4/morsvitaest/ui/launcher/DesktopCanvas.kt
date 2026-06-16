package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.saveLauncherImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ic_desk_apps
import morsvitaest.composeapp.generated.resources.ic_desk_computer
import morsvitaest.composeapp.generated.resources.ic_desk_folder
import morsvitaest.composeapp.generated.resources.ic_desk_search
import morsvitaest.composeapp.generated.resources.ic_desk_security
import morsvitaest.composeapp.generated.resources.ic_desk_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import kotlin.random.Random

// The built-in icons offered when creating/editing a custom icon.
private val builtInDesktopIcons: List<Pair<String, DrawableResource>> = listOf(
    "folder" to Res.drawable.ic_desk_folder,
    "apps" to Res.drawable.ic_desk_apps,
    "computer" to Res.drawable.ic_desk_computer,
    "search" to Res.drawable.ic_desk_search,
    "security" to Res.drawable.ic_desk_security,
    "settings" to Res.drawable.ic_desk_settings,
)

private fun iconResFor(id: String): DrawableResource? = builtInDesktopIcons.firstOrNull { it.first == id }?.second

/**
 * The desktop: a blank canvas of user-created icons and folders. Long-press an
 * empty spot for the context menu (New icon / New folder / Change wallpaper).
 * Tap an icon to launch it, a folder to open it; long-press an item to manage it.
 * Items persist in [AppSettings].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopCanvas(
    installedApps: List<InstalledApp>,
    showLabels: Boolean,
    onLaunchTarget: (String) -> Unit,
    onChangeWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = koinInject<AppSettings>()
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<DesktopItem>().also { it.addAll(settings.loadDesktopItems()) } }
    fun persist() = settings.saveDesktopItems(items.toList())

    var menuOffset by remember { mutableStateOf<Offset?>(null) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var newIconParent by remember { mutableStateOf<String?>(null) }
    var newFolderParent by remember { mutableStateOf<String?>(null) }
    var editItem by remember { mutableStateOf<DesktopItem?>(null) }

    // Picked photos route either to the New Icon draft or to an item being re-imaged.
    var draftImage by remember { mutableStateOf("") }
    var imageForItem by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            scope.launch {
                val path = saveLauncherImage("desk_${Random.nextInt(1_000_000)}.img", file.readBytes()) ?: return@launch
                val target = imageForItem
                if (target == null) {
                    draftImage = path
                } else {
                    val i = items.indexOfFirst { it.id == target }
                    if (i >= 0) {
                        items[i] = items[i].copy(imagePath = path)
                        persist()
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val maxWpx = constraints.maxWidth.toFloat()
        val maxHpx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Empty-desktop long-press → context menu.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { menuOffset = it }) },
        )

        FlowRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .widthIn(max = 380.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.filter { it.parent == "" }.forEach { item ->
                DesktopItemTile(
                    item = item,
                    installedApps = installedApps,
                    showLabels = showLabels,
                    onClick = { if (item.isFolder) openFolder = item.id else onLaunchTarget(item.target) },
                    onLongClick = { editItem = item },
                )
            }
        }

        menuOffset?.let { off ->
            // Tap-away layer to dismiss.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { menuOffset = null }) },
            )
            val menuWpx = with(density) { 200.dp.toPx() }
            val x = off.x.coerceIn(0f, (maxWpx - menuWpx).coerceAtLeast(0f))
            val y = off.y.coerceIn(0f, (maxHpx - with(density) { 170.dp.toPx() }).coerceAtLeast(0f))
            DesktopContextMenu(
                modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) },
                onNewIcon = {
                    draftImage = ""
                    imageForItem = null
                    newIconParent = ""
                    menuOffset = null
                },
                onNewFolder = {
                    newFolderParent = ""
                    menuOffset = null
                },
                onChangeWallpaper = {
                    onChangeWallpaper()
                    menuOffset = null
                },
            )
        }
    }

    openFolder?.let { fid ->
        val folder = items.firstOrNull { it.id == fid }
        FolderOverlay(
            title = folder?.label.orEmpty().ifBlank { "Folder" },
            contents = items.filter { it.parent == fid },
            installedApps = installedApps,
            onLaunch = { onLaunchTarget(it.target) },
            onLongPress = { editItem = it },
            onNewIcon = {
                draftImage = ""
                imageForItem = null
                newIconParent = fid
            },
            onClose = { openFolder = null },
        )
    }

    newIconParent?.let { parent ->
        NewIconDialog(
            installedApps = installedApps,
            draftImage = draftImage,
            onPickPhoto = {
                imageForItem = null
                imagePicker.launch()
            },
            onClearPhoto = { draftImage = "" },
            onCreate = { label, target, iconId ->
                items.add(
                    DesktopItem(
                        id = "item_${Random.nextLong()}",
                        isFolder = false,
                        label = label,
                        target = target,
                        imagePath = draftImage,
                        iconId = iconId,
                        parent = parent,
                    ),
                )
                persist()
                newIconParent = null
            },
            onDismiss = { newIconParent = null },
        )
    }

    newFolderParent?.let { parent ->
        NewFolderDialog(
            onCreate = { name ->
                items.add(DesktopItem(id = "folder_${Random.nextLong()}", isFolder = true, label = name, parent = parent))
                persist()
                newFolderParent = null
            },
            onDismiss = { newFolderParent = null },
        )
    }

    editItem?.let { item ->
        ItemMenuDialog(
            item = item,
            onRename = { newName ->
                val i = items.indexOfFirst { it.id == item.id }
                if (i >= 0) {
                    items[i] = items[i].copy(label = newName)
                    persist()
                }
                editItem = null
            },
            onChangeImage = {
                imageForItem = item.id
                imagePicker.launch()
                editItem = null
            },
            onDelete = {
                items.removeAll { it.id == item.id || it.parent == item.id }
                persist()
                editItem = null
            },
            onDismiss = { editItem = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopItemTile(
    item: DesktopItem,
    installedApps: List<InstalledApp>,
    showLabels: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            ItemGlyph(item, installedApps, Modifier.size(46.dp))
        }
        if (showLabels && item.label.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                item.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.32f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

/** Resolves an item's icon: photo → built-in icon → app icon (for app shortcuts) → default. */
@Composable
private fun ItemGlyph(item: DesktopItem, installedApps: List<InstalledApp>, modifier: Modifier) {
    val res = iconResFor(item.iconId)
    when {
        item.imagePath.isNotBlank() -> AsyncImage(
            model = "file://${item.imagePath}",
            contentDescription = item.label,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )

        item.iconId.isNotBlank() && res != null ->
            Image(painterResource(res), contentDescription = item.label, modifier = modifier)

        item.isFolder ->
            Image(painterResource(Res.drawable.ic_desk_folder), contentDescription = item.label, modifier = modifier)

        else -> {
            val app = installedApps.firstOrNull { it.packageName == item.target }
            val bmp = app?.icon
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = item.label, modifier = modifier.clip(RoundedCornerShape(12.dp)))
            } else {
                Image(painterResource(Res.drawable.ic_desk_apps), contentDescription = item.label, modifier = modifier)
            }
        }
    }
}

@Composable
private fun DesktopContextMenu(
    modifier: Modifier,
    onNewIcon: () -> Unit,
    onNewFolder: () -> Unit,
    onChangeWallpaper: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xF21A1E26))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .padding(vertical = 4.dp),
    ) {
        ContextMenuRow("New icon", onNewIcon)
        ContextMenuRow("New folder", onNewFolder)
        ContextMenuRow("Change wallpaper", onChangeWallpaper)
    }
}

@Composable
private fun ContextMenuRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderOverlay(
    title: String,
    contents: List<DesktopItem>,
    installedApps: List<InstalledApp>,
    onLaunch: (DesktopItem) -> Unit,
    onLongPress: (DesktopItem) -> Unit,
    onNewIcon: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xF2141821))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onNewIcon) { Text("New icon") }
                TextButton(onClick = onClose) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
            if (contents.isEmpty()) {
                Text("This folder is empty. Add an icon with “New icon”.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    contents.forEach { item ->
                        DesktopItemTile(
                            item = item,
                            installedApps = installedApps,
                            showLabels = true,
                            onClick = { if (!item.isFolder) onLaunch(item) },
                            onLongClick = { onLongPress(item) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewIconDialog(
    installedApps: List<InstalledApp>,
    draftImage: String,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onCreate: (label: String, target: String, iconId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var iconId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New icon") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    singleLine = true,
                    label = { Text("Opens (link / path / app)") },
                    placeholder = { Text("https://…  /root/…  com.app…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text("…or pick an installed app:", fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    installedApps.take(24).forEach { app ->
                        Text(
                            app.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    target = app.packageName
                                    if (label.isBlank()) label = app.label
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPickPhoto) { Text(if (draftImage.isNotBlank()) "Photo chosen ✓" else "Choose photo…") }
                    if (draftImage.isNotBlank()) {
                        TextButton(onClick = onClearPhoto) { Text("Clear") }
                    }
                }
                Text("…or a built-in icon:", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    builtInDesktopIcons.forEach { (id, res) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (iconId == id) 2.dp else 0.dp,
                                    color = if (iconId == id) NeverSoftAccent else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { iconId = if (iconId == id) "" else id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(painterResource(res), contentDescription = id, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() || target.isNotBlank(),
                onClick = { onCreate(label.trim().ifBlank { target.trim() }, target.trim(), iconId) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewFolderDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim()) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ItemMenuDialog(
    item: DesktopItem,
    onRename: (String) -> Unit,
    onChangeImage: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.label.ifBlank { if (item.isFolder) "Folder" else "Icon" }) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Rename") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                if (!item.isFolder) {
                    TextButton(onClick = onChangeImage) { Text("Change image…") }
                }
                TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFE2557A)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRename(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
