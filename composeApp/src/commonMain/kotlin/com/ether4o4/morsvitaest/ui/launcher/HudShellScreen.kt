package com.ether4o4.morsvitaest.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ether4o4.morsvitaest.InstalledApp
import com.ether4o4.morsvitaest.TerminalLine
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.getInstalledApps
import com.ether4o4.morsvitaest.launchApp
import com.ether4o4.morsvitaest.openUrl
import com.ether4o4.morsvitaest.ui.sandbox.SandboxSessionViewModel
import com.ether4o4.morsvitaest.ui.sandbox.SandboxTabsContent
import com.ether4o4.morsvitaest.ui.settings.SandboxUiState
import com.ether4o4.morsvitaest.ui.settings.SandboxViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.ns_mascot_alive
import morsvitaest.composeapp.generated.resources.ns_mascot_face
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val HudCyan = Color(0xFF22E0FF)
private val MatrixGlyphs = "0123456789ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾊﾋﾌﾍﾎABCDEF#$%&".toList()

/**
 * The Shell as a cyberdeck pop-up window (modeled on the COMB deck reference):
 * a floating console over the dimmed desktop — top screen shows the output,
 * the lower "keyboard deck" is where the user types, and Matrix rain streams
 * down both side rails. The real Alpine sandbox runs underneath. Opened from
 * the Computer desktop icon and the Terminal dock tile.
 */
@Composable
fun HudShellScreen(
    onClose: () -> Unit,
    onOpenChat: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenSandbox: () -> Unit = {},
    onOpenModels: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val vm = koinViewModel<SandboxViewModel>()
    val state: SandboxUiState = vm.state.collectAsStateWithLifecycle().value
    val appSettings = koinInject<AppSettings>()
    var showCheats by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var maximized by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { installedApps = getInstalledApps() }

    // The same app catalog the NeverSoft OS desktop uses, so the shell's Start
    // button opens an identical drawer.
    val catalog = remember(onOpenChat, onOpenFiles, onOpenSandbox, onOpenModels, onOpenSettings) {
        listOf(
            LauncherApp("assistant", "Assistant", null, Res.drawable.ns_mascot_face, Color(0xFF050507), onOpenChat),
            LauncherApp("terminal", "Terminal", Icons.Filled.Terminal, null, Color(0xFF2B2D31)) { showDrawer = false },
            LauncherApp("files", "Files", Icons.Filled.FolderOpen, null, Color(0xFF1C7FE0), onOpenFiles),
            LauncherApp("sandbox", "Sandbox", Icons.Filled.Inventory2, null, Color(0xFFE2557A), onOpenSandbox),
            LauncherApp("models", "Models", Icons.Filled.SmartToy, null, Color(0xFF8A6CFF), onOpenModels),
            LauncherApp("settings", "Settings", Icons.Filled.Settings, null, Color(0xFF6B7077), onOpenSettings),
            LauncherApp("internet", "Internet", Icons.Filled.Language, null, Color(0xFF1769AA)) {
                openUrl("https://www.google.com")
            },
        )
    }
    val byId = remember(catalog) { catalog.associateBy { it.id } }
    val dockPins = remember {
        appSettings.getLauncherDockPins(defaultDockPins).filter { byId.containsKey(it) }.toMutableStateList()
    }
    val startPins = remember {
        appSettings.getLauncherStartPins(defaultStartPins).filter { byId.containsKey(it) }.toMutableStateList()
    }
    fun toggleDockPin(id: String) {
        if (dockPins.contains(id)) dockPins.remove(id) else dockPins.add(id)
        appSettings.setLauncherDockPins(dockPins.toList())
    }
    fun toggleStartPin(id: String) {
        if (startPins.contains(id)) startPins.remove(id) else startPins.add(id)
        appSettings.setLauncherStartPins(startPins.toList())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() }
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        // Faint techno grid on the desktop behind the window.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val line = HudCyan.copy(alpha = 0.04f)
            var x = 0f
            while (x < size.width) {
                drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        // ——— The cyberdeck window (popup; the desktop shows behind) ———
        Box(
            modifier = Modifier
                .let {
                    if (maximized) {
                        it.fillMaxWidth(0.98f).fillMaxHeight(0.94f)
                    } else {
                        it.fillMaxWidth(0.84f).fillMaxHeight(0.6f)
                    }
                }
                .clickable(enabled = false) {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .background(Color(0xFF101317)),
            ) {
                // Title strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HudCyan.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("◢", color = HudCyan, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "NEVERSOFT // COMMAND",
                        color = HudCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    ShellClock(onClick = onOpenNotifications)
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, HudCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable { showCheats = true }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "CMDS",
                            color = HudCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ShellWinButton("—", onClick = onClose)
                    Spacer(Modifier.width(8.dp))
                    ShellWinButton(if (maximized) "❐" else "▢", onClick = { maximized = !maximized })
                    Spacer(Modifier.width(8.dp))
                    ShellWinButton("✕", onClick = onClose)
                }

                // Body: matrix rail | screen + deck | matrix rail
                Row(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    MatrixRail(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, HudCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .background(Color(0xCC020608)),
                        seed = 1,
                    )
                    Spacer(Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (state.sandboxReady) {
                            DeckTerminal(modifier = Modifier.fillMaxSize())
                        } else {
                            // Sandbox not installed yet — show the engine's
                            // install card inside the screen bezel.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, HudCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .background(Color(0xF2050B11))
                                    .padding(8.dp),
                            ) {
                                SandboxTabsContent(
                                    sandboxState = state,
                                    onSetupSandbox = { vm.onSetupSandbox() },
                                    onCancelSandbox = { vm.onCancelSandbox() },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(6.dp))
                    MatrixRail(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, HudCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .background(Color(0xCC020608)),
                        seed = 7,
                    )
                }
            }

            // The NS guy hangs out on the deck — runs across the top, leans on
            // the glass, and glitch-teleports while the shell is open.
            ActiveMascot(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .align(Alignment.TopStart)
                    .padding(top = 30.dp, start = 36.dp, end = 36.dp),
            )

            // Corner brackets on the window frame.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val len = 20.dp.toPx()
                val inset = 2.dp.toPx()
                val w = 2.5f
                val c = HudCyan
                drawLine(c, Offset(inset, inset), Offset(inset + len, inset), w)
                drawLine(c, Offset(inset, inset), Offset(inset, inset + len), w)
                drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset - len, inset), w)
                drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset, inset + len), w)
                drawLine(c, Offset(inset, size.height - inset), Offset(inset + len, size.height - inset), w)
                drawLine(c, Offset(inset, size.height - inset), Offset(inset, size.height - inset - len), w)
                drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - len, size.height - inset), w)
                drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - len), w)
            }

            // Start button in the bottom-left corner of the deck — opens the
            // identical NeverSoft OS app drawer.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF12303A), Color(0xFF0A171D))),
                    )
                    .clickable { showDrawer = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ns_mascot_face),
                    contentDescription = "Start",
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "START",
                    color = HudCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (showCheats) {
            CheatSheetOverlay(onClose = { showCheats = false })
        }

        if (showDrawer) {
            StartDrawer(
                apps = catalog,
                installedApps = installedApps,
                startPins = startPins,
                dockPins = dockPins,
                onToggleStartPin = ::toggleStartPin,
                onToggleDockPin = ::toggleDockPin,
                onLaunchPackage = { launchApp(it) },
                onClose = { showDrawer = false },
                onOpenLauncherCustomize = {
                    showDrawer = false
                    onOpenSettings()
                },
                onOpenAgentSettings = {
                    showDrawer = false
                    onOpenModels()
                },
                onLaunchChat = {
                    showDrawer = false
                    onOpenChat()
                },
            )
        }
    }
}

/**
 * The split cyberdeck terminal: top screen = output only, bottom deck = the
 * input line. Both speak to the same live sandbox session.
 */
@Composable
private fun DeckTerminal(modifier: Modifier = Modifier) {
    val session = koinViewModel<SandboxSessionViewModel>()
    val inputText by session.inputText.collectAsStateWithLifecycle()
    val isRunning by session.isRunning.collectAsStateWithLifecycle()
    val pulse by session.scrollToEndPulse.collectAsStateWithLifecycle()
    val lines = session.outputLines
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, pulse) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(modifier = modifier) {
        // ——— Top screen: output ———
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, HudCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .background(Color(0xF2030A10)),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (lines.isEmpty()) {
                    item {
                        Text(
                            ">>> SYSTEM READY — type below, output lands here",
                            color = HudCyan.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                items(lines) { line ->
                    val (prefix, color) = when (line) {
                        is TerminalLine.Command -> "C:\\> " to Color(0xFFE8FBFF)
                        is TerminalLine.Error -> "" to Color(0xFFFF6B6B)
                        else -> "" to Color(0xFF9FE8D8)
                    }
                    Text(
                        prefix + line.text,
                        color = color,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        // ——— Hinge ———
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HudCyan.copy(alpha = 0.35f)),
            )
        }

        // ——— Bottom deck: where the user types ———
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, HudCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0F14))
                .padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "C:\\>",
                    color = HudCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                TextField(
                    value = inputText,
                    onValueChange = session::setInputText,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color(0xFFE8FBFF),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    placeholder = {
                        Text(
                            "enter command…",
                            color = HudCyan.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { session.submit() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = HudCyan,
                    ),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .background(if (isRunning) Color(0xFF3A1518) else HudCyan.copy(alpha = 0.10f))
                        .clickable { if (isRunning) session.cancelRunning() else session.submit() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (isRunning) "STOP" else "RUN",
                        color = if (isRunning) Color(0xFFFF6B6B) else HudCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ————— Command cheat sheet —————

private val cheatGuide = """
HOW TO GET AROUND
You start in /root (your home). The prompt shows where you are.
  ls            see what's in this folder
  cd name       step INTO a folder        cd ..   step back OUT
  pwd           print where you are right now
  ls -la        list EVERYTHING here, hidden files + sizes too

HOW TO FIND THINGS
  find / -name "*.txt"          hunt the whole system for .txt files
  find . -name "notes*"         hunt from HERE down for names starting with notes
  grep -r "password" .          search inside every file here for a word
  which python3                 where does a command live?
  du -sh *                      what's taking up space in this folder
""".trimIndent()

private val cheatSections: List<Pair<String, List<Pair<String, String>>>> = listOf(
    "NAVIGATE" to listOf(
        "pwd" to "where am I",
        "ls" to "list files here",
        "ls -la" to "list all + hidden + details",
        "cd dir" to "enter a folder",
        "cd .." to "go up one level",
        "cd ~ | cd /" to "jump home | jump to root",
        "tree" to "folder map (apk add tree)",
    ),
    "FILES & FOLDERS" to listOf(
        "cat file" to "print a file",
        "less file" to "scroll a file (q quits)",
        "head/tail file" to "first / last lines",
        "tail -f log" to "watch a file live",
        "touch file" to "make an empty file",
        "mkdir -p a/b" to "make folders (nested)",
        "cp src dst" to "copy  (cp -r for folders)",
        "mv src dst" to "move / rename",
        "rm file" to "delete  (rm -rf dir = folder, careful)",
        "ln -s tgt name" to "make a shortcut (symlink)",
        "nano file" to "edit a file (apk add nano)",
        "vi file" to "edit (i=insert, Esc :wq=save+quit)",
    ),
    "FIND & SEARCH" to listOf(
        "find . -name \"*.log\"" to "find by name from here",
        "find / -size +10M" to "find big files",
        "find . -mtime -1" to "changed in last day",
        "grep word file" to "search inside a file",
        "grep -ri word ." to "search all files, any case",
        "grep -rn word ." to "…with line numbers",
        "which cmd" to "path of a command",
        "du -sh *" to "sizes of everything here",
        "df -h" to "disk space overall",
    ),
    "PACKAGES (Alpine)" to listOf(
        "apk update" to "refresh package index",
        "apk add pkg" to "install (e.g. apk add htop)",
        "apk del pkg" to "uninstall",
        "apk search word" to "find a package",
        "apk info pkg" to "package details",
    ),
    "PROCESSES & SYSTEM" to listOf(
        "ps aux" to "everything running",
        "top" to "live process monitor (q quits)",
        "kill PID" to "stop a process (kill -9 = force)",
        "free -m" to "memory usage",
        "uptime" to "load + time up",
        "uname -a" to "system info",
        "date" to "current date/time",
        "clear" to "wipe the screen",
        "history" to "your past commands",
    ),
    "NETWORK" to listOf(
        "ping host" to "is it reachable (Ctrl+C stops)",
        "curl url" to "fetch a page/API",
        "curl -O url" to "download a file",
        "wget url" to "download (alt)",
        "curl -s api | jq" to "pretty-print JSON",
        "ssh user@host" to "remote shell",
        "scp file user@host:~" to "copy file to a server",
        "ip addr" to "my addresses",
    ),
    "ARCHIVES" to listOf(
        "tar -czf out.tgz dir" to "zip a folder up",
        "tar -xzf file.tgz" to "unpack it",
        "unzip file.zip" to "unpack zip (apk add unzip)",
        "gzip / gunzip file" to "compress / decompress",
    ),
    "PERMISSIONS" to listOf(
        "chmod +x script.sh" to "make runnable",
        "chmod 644 file" to "rw for you, read for rest",
        "chown user file" to "change owner",
        "whoami" to "who am I",
    ),
    "SCRIPTING & DEV" to listOf(
        "python3 file.py" to "run python",
        "python3 -m http.server" to "instant web server",
        "node file.js" to "run javascript",
        "pip install pkg" to "python packages",
        "git clone url" to "grab a repo",
        "git status / log" to "repo state / history",
        "bash script.sh" to "run a shell script",
        "echo \"x\" > f / >> f" to "overwrite / append to file",
        "cmd1 | cmd2" to "pipe output into next command",
        "cmd > out.txt 2>&1" to "save all output to a file",
        "ctrl+c" to "stop the running command",
    ),
)

/** Scrollable HUD-styled command cheat sheet over the console. */
@Composable
private fun CheatSheetOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2010407))
            .clickable { onClose() }
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .background(Color(0xFF030A10))
                .clickable(enabled = false) {},
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudCyan.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "◢ COMMAND CHEAT SHEET",
                    color = HudCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, HudCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = HudCyan, fontSize = 12.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    cheatGuide,
                    color = Color(0xFFB8F4E6),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(14.dp))
                cheatSections.forEach { (title, rows) ->
                    Text(
                        "── $title ${"─".repeat((28 - title.length).coerceAtLeast(2))}",
                        color = HudCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                    rows.forEach { (cmd, desc) ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text(
                                cmd,
                                color = Color(0xFFE8FBFF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(170.dp),
                            )
                            Text(
                                desc,
                                color = HudCyan.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ShellWinButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, HudCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = HudCyan, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ShellClock(onClick: () -> Unit) {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(15_000)
        }
    }
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val h = local.hour
    val h12 = if (h % 12 == 0) 12 else h % 12
    val ampm = if (h < 12) "AM" else "PM"
    Text(
        "$h12:${local.minute.toString().padStart(2, '0')} $ampm",
        color = HudCyan,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/**
 * The NS mascot living on the shell: a feathered cutout (no block) that runs
 * along the top of the glass with a squash/stretch walk, leans against the
 * rail, then glitch-teleports (flicker + ghost) to a new spot.
 */
@Composable
private fun ActiveMascot(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val mascotPx = with(density) { 96.dp.toPx() }
        val maxX = (widthPx - mascotPx).coerceAtLeast(1f)
        val x = remember { Animatable(0f) }
        var flipped by remember { mutableStateOf(false) }
        var leaning by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(true) }
        var visible by remember { mutableStateOf(true) }
        var ghost by remember { mutableStateOf(false) }

        // Fast little bounce that sells the "running" cycle.
        val runCycle = rememberInfiniteTransition()
        val bob by runCycle.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        )

        LaunchedEffect(maxX) {
            while (true) {
                // Sprint to the right rail.
                running = true
                leaning = false
                flipped = false
                x.animateTo(maxX, tween((maxX * 8).toInt().coerceAtLeast(1500), easing = LinearEasing))
                // Lean against the clear wall of code.
                running = false
                leaning = true
                delay(2800)
                // Glitch across the screen — flicker, ghost, teleport.
                leaning = false
                ghost = true
                repeat(3) {
                    visible = false
                    delay(70)
                    x.snapTo(Random.nextFloat() * maxX)
                    visible = true
                    delay(110)
                }
                ghost = false
                // Sprint back to the left rail.
                running = true
                flipped = true
                x.animateTo(0f, tween((maxX * 8).toInt().coerceAtLeast(1500), easing = LinearEasing))
                running = false
                leaning = true
                delay(2200)
                leaning = false
                ghost = true
                repeat(2) {
                    visible = false
                    delay(70)
                    x.snapTo(Random.nextFloat() * maxX)
                    visible = true
                    delay(110)
                }
                ghost = false
                x.snapTo(0f)
                flipped = false
            }
        }

        // Walk bob + squash/stretch: he compresses on the down-step and
        // stretches on the up-step, so the legs read as actually moving.
        val yBob = if (running) bob * 9f else 0f
        val squashY = if (running) 1f + (bob - 0.5f) * 0.10f else 1f
        val squashX = if (running) 1f - (bob - 0.5f) * 0.10f else 1f
        val tilt = when {
            leaning -> if (flipped) -12f else 12f
            running -> (bob - 0.5f) * 6f
            else -> 0f
        }

        if (ghost) {
            Image(
                painter = painterResource(Res.drawable.ns_mascot_alive),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .offset { IntOffset((x.value + 10f).toInt(), yBob.toInt()) }
                    .graphicsLayer {
                        alpha = 0.22f
                        scaleX = if (flipped) -1f else 1f
                    },
            )
        }
        if (visible) {
            Image(
                painter = painterResource(Res.drawable.ns_mascot_alive),
                contentDescription = "NS",
                modifier = Modifier
                    .size(96.dp)
                    .offset { IntOffset(x.value.toInt(), yBob.toInt()) }
                    .graphicsLayer {
                        scaleX = (if (flipped) -1f else 1f) * squashX
                        scaleY = squashY
                        rotationZ = tilt
                    },
            )
        }
    }
}

/**
 * A narrow vertical rail of Matrix rain: per-column falling glyph streams with
 * a bright head and a fading tail, shimmering as they fall.
 */
@Composable
private fun MatrixRail(modifier: Modifier, seed: Int, columns: Int = 2) {
    val textMeasurer = rememberTextMeasurer()
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos = it }
        }
    }
    // Deterministic per-column speeds/phases from the seed.
    val speeds = remember(seed) { List(columns) { 90f + ((seed * 37 + it * 53) % 90) } }
    val phases = remember(seed) { List(columns) { ((seed * 101 + it * 211) % 1000) * 7f } }
    val glyphStyle = remember {
        TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }

    Canvas(modifier = modifier) {
        val t = frameNanos / 1_000_000_000f
        val cellH = 14.dp.toPx()
        val colW = size.width / columns
        val tail = 9
        for (c in 0 until columns) {
            val travel = size.height + tail * cellH
            val headY = ((t * speeds[c] + phases[c]) % travel) - tail * cellH / 2f
            for (i in 0 until tail) {
                val y = headY - i * cellH
                if (y < -cellH || y > size.height) continue
                // Shimmering glyph choice, deterministic per cell+tick.
                val tick = (t * 9).toInt()
                val g = MatrixGlyphs[((y / cellH).toInt() * 31 + c * 17 + seed * 13 + tick * 7).mod(MatrixGlyphs.size)]
                val alpha = if (i == 0) 0.95f else (0.75f - i * 0.08f).coerceAtLeast(0.08f)
                val color = if (i == 0) Color(0xFFCFFFF6) else HudCyan.copy(alpha = alpha)
                drawText(
                    textMeasurer = textMeasurer,
                    text = g.toString(),
                    topLeft = Offset(c * colW + colW * 0.22f, y),
                    style = glyphStyle.copy(color = color),
                )
            }
        }
    }
}
