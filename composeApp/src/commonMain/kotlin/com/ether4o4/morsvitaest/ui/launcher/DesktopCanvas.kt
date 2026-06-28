package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import morsvitaest.composeapp.generated.resources.ic_desk_documents
import morsvitaest.composeapp.generated.resources.ic_desk_folder
import morsvitaest.composeapp.generated.resources.ic_desk_internet
import morsvitaest.composeapp.generated.resources.ic_desk_search
import morsvitaest.composeapp.generated.resources.ic_desk_security
import morsvitaest.composeapp.generated.resources.ic_desk_settings
import morsvitaest.composeapp.generated.resources.ic_desk_trash
import morsvitaest.composeapp.generated.resources.ic_desk_wifi
import morsvitaest.composeapp.generated.resources.ic_glass_camera
import morsvitaest.composeapp.generated.resources.ic_glass_messages
import morsvitaest.composeapp.generated.resources.ic_glass_phone
import morsvitaest.composeapp.generated.resources.ic_glass_terminal
import morsvitaest.composeapp.generated.resources.ic_pack_01
import morsvitaest.composeapp.generated.resources.ic_pack_02
import morsvitaest.composeapp.generated.resources.ic_pack_03
import morsvitaest.composeapp.generated.resources.ic_pack_04
import morsvitaest.composeapp.generated.resources.ic_pack_05
import morsvitaest.composeapp.generated.resources.ic_pack_06
import morsvitaest.composeapp.generated.resources.ic_pack_07
import morsvitaest.composeapp.generated.resources.ic_pack_08
import morsvitaest.composeapp.generated.resources.ic_pack_09
import morsvitaest.composeapp.generated.resources.ic_pack_10
import morsvitaest.composeapp.generated.resources.ic_pack_11
import morsvitaest.composeapp.generated.resources.ic_pack_12
import morsvitaest.composeapp.generated.resources.ic_pack_13
import morsvitaest.composeapp.generated.resources.ic_pack_14
import morsvitaest.composeapp.generated.resources.ic_pack_15
import morsvitaest.composeapp.generated.resources.ic_pack_16
import morsvitaest.composeapp.generated.resources.ic_pack_17
import morsvitaest.composeapp.generated.resources.ic_pack_18
import morsvitaest.composeapp.generated.resources.ic_pack_19
import morsvitaest.composeapp.generated.resources.ic_pack_20
import morsvitaest.composeapp.generated.resources.ic_pack_21
import morsvitaest.composeapp.generated.resources.ic_pack_22
import morsvitaest.composeapp.generated.resources.ic_pack_23
import morsvitaest.composeapp.generated.resources.ic_pack_24
import morsvitaest.composeapp.generated.resources.ic_pack_25
import morsvitaest.composeapp.generated.resources.ic_pack_26
import morsvitaest.composeapp.generated.resources.ic_pack_27
import morsvitaest.composeapp.generated.resources.ic_pack_28
import morsvitaest.composeapp.generated.resources.ic_pack_29
import morsvitaest.composeapp.generated.resources.ic_pack_30
import morsvitaest.composeapp.generated.resources.ic_pack_31
import morsvitaest.composeapp.generated.resources.ic_pack_32
import morsvitaest.composeapp.generated.resources.ic_pack_33
import morsvitaest.composeapp.generated.resources.ic_pack_34
import morsvitaest.composeapp.generated.resources.ic_pack_35
import morsvitaest.composeapp.generated.resources.ic_pack_36
import morsvitaest.composeapp.generated.resources.ic_pack_37
import morsvitaest.composeapp.generated.resources.ic_pack_38
import morsvitaest.composeapp.generated.resources.ic_pack_39
import morsvitaest.composeapp.generated.resources.ic_pack_40
import morsvitaest.composeapp.generated.resources.ic_pack_41
import morsvitaest.composeapp.generated.resources.ic_pack_42
import morsvitaest.composeapp.generated.resources.ic_pack_43
import morsvitaest.composeapp.generated.resources.ic_pack_44
import morsvitaest.composeapp.generated.resources.ic_pack_45
import morsvitaest.composeapp.generated.resources.ic_pack_46
import morsvitaest.composeapp.generated.resources.ic_pack_47
import morsvitaest.composeapp.generated.resources.ic_pack_48
import morsvitaest.composeapp.generated.resources.ic_pack_49
import morsvitaest.composeapp.generated.resources.ic_pack_50
import morsvitaest.composeapp.generated.resources.ic_pack_51
import morsvitaest.composeapp.generated.resources.ic_pack_52
import morsvitaest.composeapp.generated.resources.ic_pack_53
import morsvitaest.composeapp.generated.resources.ic_pack_54
import morsvitaest.composeapp.generated.resources.ic_pack_55
import morsvitaest.composeapp.generated.resources.ic_pack_56
import morsvitaest.composeapp.generated.resources.ic_pack_57
import morsvitaest.composeapp.generated.resources.ic_pack_58
import morsvitaest.composeapp.generated.resources.ic_pack_59
import morsvitaest.composeapp.generated.resources.ic_pack_60
import morsvitaest.composeapp.generated.resources.ic_pack_61
import morsvitaest.composeapp.generated.resources.ic_pack_62
import morsvitaest.composeapp.generated.resources.ic_pack_63
import morsvitaest.composeapp.generated.resources.ic_pack_64
import morsvitaest.composeapp.generated.resources.ic_pack_65
import morsvitaest.composeapp.generated.resources.ic_pack_66
import morsvitaest.composeapp.generated.resources.ic_pack_67
import morsvitaest.composeapp.generated.resources.ic_pack_68
import morsvitaest.composeapp.generated.resources.ic_pack_69
import morsvitaest.composeapp.generated.resources.ic_pack_70
import morsvitaest.composeapp.generated.resources.ic_pack_71
import morsvitaest.composeapp.generated.resources.ic_pack_72
import morsvitaest.composeapp.generated.resources.ic_pack_73
import morsvitaest.composeapp.generated.resources.ic_pack_74
import morsvitaest.composeapp.generated.resources.ic_pack_75
import morsvitaest.composeapp.generated.resources.ic_pack_76
import morsvitaest.composeapp.generated.resources.ic_pack_77
import morsvitaest.composeapp.generated.resources.ic_pack_78
import morsvitaest.composeapp.generated.resources.ic_pack_79
import morsvitaest.composeapp.generated.resources.ic_pack_80
import morsvitaest.composeapp.generated.resources.ic_pack_81
import morsvitaest.composeapp.generated.resources.ic_pack_82
import morsvitaest.composeapp.generated.resources.ic_pack_83
import morsvitaest.composeapp.generated.resources.ic_pack_84
import morsvitaest.composeapp.generated.resources.ic_pack_85
import morsvitaest.composeapp.generated.resources.ic_pack_86
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

// The built-in icons offered when creating/editing a custom icon.
private val builtInDesktopIcons: List<Pair<String, DrawableResource>> = listOf(
    "folder" to Res.drawable.ic_desk_folder,
    "apps" to Res.drawable.ic_desk_apps,
    "computer" to Res.drawable.ic_desk_computer,
    "documents" to Res.drawable.ic_desk_documents,
    "internet" to Res.drawable.ic_desk_internet,
    "search" to Res.drawable.ic_desk_search,
    "security" to Res.drawable.ic_desk_security,
    "settings" to Res.drawable.ic_desk_settings,
    "trash" to Res.drawable.ic_desk_trash,
    "wifi" to Res.drawable.ic_desk_wifi,
    "camera" to Res.drawable.ic_glass_camera,
    "messages" to Res.drawable.ic_glass_messages,
    "phone" to Res.drawable.ic_glass_phone,
    "terminal" to Res.drawable.ic_glass_terminal,
    "mascot" to Res.drawable.ns_mascot_face,
)

// The user's bundled custom icon pack (drawables named ic_pack_*).
private val customIconPack: List<Pair<String, DrawableResource>> = listOf(
    "ic_pack_01" to Res.drawable.ic_pack_01,
    "ic_pack_02" to Res.drawable.ic_pack_02,
    "ic_pack_03" to Res.drawable.ic_pack_03,
    "ic_pack_04" to Res.drawable.ic_pack_04,
    "ic_pack_05" to Res.drawable.ic_pack_05,
    "ic_pack_06" to Res.drawable.ic_pack_06,
    "ic_pack_07" to Res.drawable.ic_pack_07,
    "ic_pack_08" to Res.drawable.ic_pack_08,
    "ic_pack_09" to Res.drawable.ic_pack_09,
    "ic_pack_10" to Res.drawable.ic_pack_10,
    "ic_pack_11" to Res.drawable.ic_pack_11,
    "ic_pack_12" to Res.drawable.ic_pack_12,
    "ic_pack_13" to Res.drawable.ic_pack_13,
    "ic_pack_14" to Res.drawable.ic_pack_14,
    "ic_pack_15" to Res.drawable.ic_pack_15,
    "ic_pack_16" to Res.drawable.ic_pack_16,
    "ic_pack_17" to Res.drawable.ic_pack_17,
    "ic_pack_18" to Res.drawable.ic_pack_18,
    "ic_pack_19" to Res.drawable.ic_pack_19,
    "ic_pack_20" to Res.drawable.ic_pack_20,
    "ic_pack_21" to Res.drawable.ic_pack_21,
    "ic_pack_22" to Res.drawable.ic_pack_22,
    "ic_pack_23" to Res.drawable.ic_pack_23,
    "ic_pack_24" to Res.drawable.ic_pack_24,
    "ic_pack_25" to Res.drawable.ic_pack_25,
    "ic_pack_26" to Res.drawable.ic_pack_26,
    "ic_pack_27" to Res.drawable.ic_pack_27,
    "ic_pack_28" to Res.drawable.ic_pack_28,
    "ic_pack_29" to Res.drawable.ic_pack_29,
    "ic_pack_30" to Res.drawable.ic_pack_30,
    "ic_pack_31" to Res.drawable.ic_pack_31,
    "ic_pack_32" to Res.drawable.ic_pack_32,
    "ic_pack_33" to Res.drawable.ic_pack_33,
    "ic_pack_34" to Res.drawable.ic_pack_34,
    "ic_pack_35" to Res.drawable.ic_pack_35,
    "ic_pack_36" to Res.drawable.ic_pack_36,
    "ic_pack_37" to Res.drawable.ic_pack_37,
    "ic_pack_38" to Res.drawable.ic_pack_38,
    "ic_pack_39" to Res.drawable.ic_pack_39,
    "ic_pack_40" to Res.drawable.ic_pack_40,
    "ic_pack_41" to Res.drawable.ic_pack_41,
    "ic_pack_42" to Res.drawable.ic_pack_42,
    "ic_pack_43" to Res.drawable.ic_pack_43,
    "ic_pack_44" to Res.drawable.ic_pack_44,
    "ic_pack_45" to Res.drawable.ic_pack_45,
    "ic_pack_46" to Res.drawable.ic_pack_46,
    "ic_pack_47" to Res.drawable.ic_pack_47,
    "ic_pack_48" to Res.drawable.ic_pack_48,
    "ic_pack_49" to Res.drawable.ic_pack_49,
    "ic_pack_50" to Res.drawable.ic_pack_50,
    "ic_pack_51" to Res.drawable.ic_pack_51,
    "ic_pack_52" to Res.drawable.ic_pack_52,
    "ic_pack_53" to Res.drawable.ic_pack_53,
    "ic_pack_54" to Res.drawable.ic_pack_54,
    "ic_pack_55" to Res.drawable.ic_pack_55,
    "ic_pack_56" to Res.drawable.ic_pack_56,
    "ic_pack_57" to Res.drawable.ic_pack_57,
    "ic_pack_58" to Res.drawable.ic_pack_58,
    "ic_pack_59" to Res.drawable.ic_pack_59,
    "ic_pack_60" to Res.drawable.ic_pack_60,
    "ic_pack_61" to Res.drawable.ic_pack_61,
    "ic_pack_62" to Res.drawable.ic_pack_62,
    "ic_pack_63" to Res.drawable.ic_pack_63,
    "ic_pack_64" to Res.drawable.ic_pack_64,
    "ic_pack_65" to Res.drawable.ic_pack_65,
    "ic_pack_66" to Res.drawable.ic_pack_66,
    "ic_pack_67" to Res.drawable.ic_pack_67,
    "ic_pack_68" to Res.drawable.ic_pack_68,
    "ic_pack_69" to Res.drawable.ic_pack_69,
    "ic_pack_70" to Res.drawable.ic_pack_70,
    "ic_pack_71" to Res.drawable.ic_pack_71,
    "ic_pack_72" to Res.drawable.ic_pack_72,
    "ic_pack_73" to Res.drawable.ic_pack_73,
    "ic_pack_74" to Res.drawable.ic_pack_74,
    "ic_pack_75" to Res.drawable.ic_pack_75,
    "ic_pack_76" to Res.drawable.ic_pack_76,
    "ic_pack_77" to Res.drawable.ic_pack_77,
    "ic_pack_78" to Res.drawable.ic_pack_78,
    "ic_pack_79" to Res.drawable.ic_pack_79,
    "ic_pack_80" to Res.drawable.ic_pack_80,
    "ic_pack_81" to Res.drawable.ic_pack_81,
    "ic_pack_82" to Res.drawable.ic_pack_82,
    "ic_pack_83" to Res.drawable.ic_pack_83,
    "ic_pack_84" to Res.drawable.ic_pack_84,
    "ic_pack_85" to Res.drawable.ic_pack_85,
    "ic_pack_86" to Res.drawable.ic_pack_86,
)

private fun iconResFor(id: String): DrawableResource? = builtInDesktopIcons.firstOrNull { it.first == id }?.second
    ?: customIconPack.firstOrNull { it.first == id }?.second

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
    reloadKey: Int = 0,
) {
    val settings = koinInject<AppSettings>()
    val scope = rememberCoroutineScope()
    // reloadKey changes (e.g. a shortcut added from the Start menu) reload from storage.
    val items = remember(reloadKey) { mutableStateListOf<DesktopItem>().also { it.addAll(settings.loadDesktopItems()) } }
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

        // Desktop icons are freely placed. A saved spot is stored as a fraction of the
        // canvas (xFrac/yFrac); items never placed fall back to an auto grid by index.
        // Long-press to pick one up and drag it anywhere; long-press without moving edits it.
        val tileWpx = with(density) { 80.dp.toPx() }
        val tileHpx = with(density) { 96.dp.toPx() }
        val padPx = with(density) { 12.dp.toPx() }
        val cols = ((maxWpx - padPx) / tileWpx).toInt().coerceAtLeast(1)

        items.filter { it.parent == "" }.forEachIndexed { index, item ->
            fun gridX() = padPx + (index % cols) * tileWpx
            fun gridY() = padPx + (index / cols) * tileHpx
            val px = if (item.xFrac >= 0f) item.xFrac * maxWpx else gridX()
            val py = if (item.yFrac >= 0f) item.yFrac * maxHpx else gridY()
            Box(
                modifier = Modifier
                    .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                    .pointerInput(item.id, maxWpx, maxHpx, cols) {
                        // Only count it as a MOVE once the finger travels a clear distance.
                        // A long-press with little/no drag opens the item menu (rename / change
                        // image / delete) instead — so removing an icon is reliable and isn't
                        // hijacked by tiny finger jitter being treated as a drag.
                        val moveThresholdPx = 20.dp.toPx()
                        var totalDist = 0f
                        var dragging = false
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                totalDist = 0f
                                dragging = false
                            },
                            onDragEnd = { if (dragging) persist() else editItem = item },
                            onDragCancel = { if (dragging) persist() },
                        ) { change, drag ->
                            change.consume()
                            totalDist += abs(drag.x) + abs(drag.y)
                            if (!dragging && totalDist >= moveThresholdPx) dragging = true
                            if (dragging) {
                                val i = items.indexOfFirst { it.id == item.id }
                                if (i >= 0) {
                                    val cur = items[i]
                                    val curX = if (cur.xFrac >= 0f) cur.xFrac * maxWpx else gridX()
                                    val curY = if (cur.yFrac >= 0f) cur.yFrac * maxHpx else gridY()
                                    val nx = (curX + drag.x).coerceIn(0f, (maxWpx - tileWpx).coerceAtLeast(0f))
                                    val ny = (curY + drag.y).coerceIn(0f, (maxHpx - tileHpx).coerceAtLeast(0f))
                                    items[i] = cur.copy(xFrac = nx / maxWpx, yFrac = ny / maxHpx)
                                }
                            }
                        }
                    },
            ) {
                DesktopItemTile(
                    item = item,
                    installedApps = installedApps,
                    showLabels = showLabels,
                    onClick = { if (item.isFolder) openFolder = item.id else onLaunchTarget(item.target) },
                    onLongClick = null,
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
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                // When long-press is handled by an outer drag wrapper, the tile must not
                // claim long-press itself, or the two gestures fight.
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
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
    var appQuery by remember { mutableStateOf("") }
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
                OutlinedTextField(
                    value = appQuery,
                    onValueChange = { appQuery = it },
                    singleLine = true,
                    label = { Text("Search apps") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    installedApps
                        .filter { appQuery.isBlank() || it.label.contains(appQuery, ignoreCase = true) }
                        .forEach { app ->
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (builtInDesktopIcons + customIconPack).forEach { (id, res) ->
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
                TextButton(onClick = onDelete) { Text("Remove from home", color = Color(0xFFE2557A)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRename(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
