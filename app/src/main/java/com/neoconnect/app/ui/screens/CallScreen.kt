package com.neoconnect.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.neoconnect.app.webrtc.CallManager
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.TimeUnit

data class BgFilter(val name: String, val color: Color)

@Composable
fun CallScreen(roomId: String, isVideoCall: Boolean, onCallEnd: () -> Unit) {
    val context = LocalContext.current

    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isRemoteVideoAdded by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    var isCallConnected by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0L) }

    // Sheet states
    var showMoreSheet by remember { mutableStateOf(false) }
    var showBgSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Normal") }

    val bgFilters = listOf(
        BgFilter("Normal", Color.White),
        BgFilter("Cool", Color(0xFFBBDEFB)),
        BgFilter("Warm", Color(0xFFFFE0B2)),
        BgFilter("B&W", Color(0xFF9E9E9E)),
        BgFilter("Vintage", Color(0xFFD7CCC8)),
        BgFilter("Vivid", Color(0xFFE1BEE7)),
    )

    val callManager = remember { CallManager(context) }
    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val proximityWakeLock = remember {
        powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "NeoConnect::ProximityLock")
    }

    DisposableEffect(Unit) {
        if (isVideoCall) {
            (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (!proximityWakeLock.isHeld) proximityWakeLock.acquire()
        }
        onDispose {
            (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (proximityWakeLock.isHeld) proximityWakeLock.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) startCall(callManager, localView, remoteView, roomId, isVideoCall)
    }

    LaunchedEffect(Unit) {
        callManager.onRemoteStream = { track ->
            Handler(Looper.getMainLooper()).post {
                try { track.setEnabled(true); track.addSink(remoteView); isRemoteVideoAdded = true } catch (e: Exception) {}
            }
        }
        callManager.onCallEnded = { Handler(Looper.getMainLooper()).post { onCallEnd() } }
        callManager.onConnected = { Handler(Looper.getMainLooper()).post { isCallConnected = true } }

        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            startCall(callManager, localView, remoteView, roomId, isVideoCall)
        } else {
            permissionLauncher.launch(perms)
        }
    }

    LaunchedEffect(isCallConnected) {
        if (isCallConnected) while (true) { delay(1000L); callDuration++ }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Background
        if (!isVideoCall || !isRemoteVideoAdded) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        // Remote Video
        if (isVideoCall) {
            AndroidView(factory = { remoteView }, modifier = Modifier.fillMaxSize())
            if (!isRemoteVideoAdded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            }
        }

        // Local Video PiP
        if (isVideoCall) {
            AndroidView(
                factory = { localView },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(110.dp, 160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { callManager.switchCamera() }
            )
        }

        // Audio UI
        if (!isVideoCall) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Person, "Audio", Modifier.size(100.dp), Color.White)
                Spacer(Modifier.height(16.dp))
                Text(formatDuration(callDuration), color = Color.White, fontSize = 24.sp)
            }
        }

        // Timer
        if (isVideoCall && callDuration > 0L) {
            Text(
                formatDuration(callDuration),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Control Bar
        ControlBar(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            isMuted = isMuted,
            isVideoOff = isVideoOff,
            isSpeakerOn = isSpeakerOn,
            isVideoCall = isVideoCall,
            onMuteToggle = { isMuted = !isMuted; callManager.toggleMute(isMuted) },
            onVideoToggle = { isVideoOff = !isVideoOff; callManager.toggleCamera(isVideoOff) },
            onSpeakerToggle = { isSpeakerOn = !isSpeakerOn; callManager.enableSpeaker(isSpeakerOn) },
            onEndCall = { callManager.endCall(); onCallEnd() },
            onMoreClick = { showMoreSheet = true },
            isVideoCall = isVideoCall
        )

        // More Options Bottom Sheet
        if (showMoreSheet) {
            MoreOptionsSheet(
                onDismiss = { showMoreSheet = false },
                onScreenShare = { callManager.switchCamera(); showMoreSheet = false },
                onBackground = { showMoreSheet = false; showBgSheet = true }
            )
        }

        // Background Filter Sheet
        if (showBgSheet) {
            BackgroundFilterSheet(
                filters = bgFilters,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                onDismiss = { showBgSheet = false }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { localView.release() } catch (e: Exception) {}
            try { remoteView.release() } catch (e: Exception) {}
            callManager.endCall()
        }
    }
}

// ── More Options Bottom Sheet ─────────────────────────────────────────────────

@Composable
fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onScreenShare: () -> Unit,
    onBackground: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(bottom = 36.dp)
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                // Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 20.dp)
                        .size(36.dp, 4.dp)
                        .background(Color(0xFF48484A), CircleShape)
                )

                // Group 1
                SheetGroup {
                    SheetItem(icon = Icons.Default.People, label = "Participants") { onDismiss() }
                    SheetDivider()
                    SheetItem(icon = Icons.Default.ScreenShare, label = "Share Screen") { onScreenShare() }
                    SheetDivider()
                    SheetItem(icon = Icons.Default.FiberManualRecord, label = "Record") { onDismiss() }
                }

                Spacer(Modifier.height(10.dp))

                // Group 2
                SheetGroup {
                    SheetItem(icon = Icons.Default.PersonOutline, label = "Background") { onBackground() }
                    SheetDivider()
                    SheetItem(icon = Icons.Default.ClosedCaption, label = "Subtitles") { onDismiss() }
                    SheetDivider()
                    SheetItem(icon = Icons.Default.Description, label = "Call Transcript") { onDismiss() }
                }
            }
        }
    }
}

@Composable
fun SheetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF2C2C2E)),
        content = content
    )
}

@Composable
fun SheetDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(0.5.dp)
            .background(Color(0xFF3A3A3C))
    )
}

@Composable
fun SheetItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(22.dp))
            Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Normal)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF48484A), modifier = Modifier.size(18.dp))
    }
}

// ── Background Filter Sheet ───────────────────────────────────────────────────

@Composable
fun BackgroundFilterSheet(
    filters: List<BgFilter>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(bottom = 36.dp)
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                // Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .size(36.dp, 4.dp)
                        .background(Color(0xFF48484A), CircleShape)
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A3C))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(16.dp))
                    }
                }

                // None option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .clickable { onFilterSelected("None") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(1.dp, Color(0xFF48484A), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
                        }
                        Text("None", color = Color.White, fontSize = 16.sp)
                    }
                    if (selectedFilter == "None") {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF30D158), modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Filter Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                ) {
                    Text(
                        "Color Filters",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(filters) { filter ->
                            BgFilterItem(
                                filter = filter,
                                isSelected = selectedFilter == filter.name,
                                onClick = { onFilterSelected(filter.name) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Blur option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .clickable { onFilterSelected("Blur") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF48484A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BlurOn, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Blur Background", color = Color.White, fontSize = 16.sp)
                            Text("Portrait mode", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }
                    if (selectedFilter == "Blur") {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF30D158), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BgFilterItem(filter: BgFilter, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(filter.color)
                .then(
                    if (isSelected) Modifier.border(3.dp, Color(0xFF0A84FF), CircleShape)
                    else Modifier.border(1.dp, Color(0xFF48484A), CircleShape)
                )
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(filter.name, color = if (isSelected) Color(0xFF0A84FF) else Color(0xFF8E8E93), fontSize = 11.sp)
    }
}

// ── Control Bar ───────────────────────────────────────────────────────────────

@Composable
fun ControlBar(
    modifier: Modifier,
    isMuted: Boolean,
    isVideoOff: Boolean,
    isSpeakerOn: Boolean,
    isVideoCall: Boolean,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            background = if (isMuted) Color(0xFF636366) else Color(0xFF636366),
            iconTint = if (isMuted) Color.White else Color.White,
            onClick = onMuteToggle
        )
        ControlButton(
            icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
            background = Color(0xFF636366),
            onClick = onVideoToggle
        )
        ControlButton(
            icon = Icons.Default.MoreHoriz,
            background = Color(0xFF636366),
            onClick = onMoreClick
        )
        ControlButton(
            icon = Icons.Default.CallEnd,
            background = Color(0xFFFF3B30),
            onClick = onEndCall
        )
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    background: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun startCall(
    manager: CallManager,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer,
    roomId: String,
    isVideoCall: Boolean
) {
    manager.init()
    val eglContext = manager.eglBase?.eglBaseContext ?: return
    try { remoteView.init(eglContext, null); remoteView.setZOrderMediaOverlay(false); remoteView.setMirror(false) } catch (e: Exception) {}
    manager.createStream(isVideoCall)
    if (isVideoCall) {
        try { localView.init(eglContext, null); localView.setZOrderMediaOverlay(true); localView.setMirror(true) } catch (e: Exception) {}
        manager.startLocalVideoCapture(localView)
    }
    manager.joinRoom(roomId)
}

fun formatDuration(seconds: Long): String {
    val mins = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds - TimeUnit.MINUTES.toSeconds(mins)
    return String.format("%02d:%02d", mins, secs)
}
