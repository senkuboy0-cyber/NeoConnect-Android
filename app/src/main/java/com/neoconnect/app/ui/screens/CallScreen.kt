package com.neoconnect.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.neoconnect.app.webrtc.CallManager
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.TimeUnit

data class MenuItem(val title: String, val icon: ImageVector, val tint: Color)

@Composable
fun CallScreen(roomId: String, isVideoCall: Boolean, onCallEnd: () -> Unit) {
    val context = LocalContext.current
    
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isRemoteVideoAdded by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    var isCallConnected by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0L) }
    
    // Menu State
    var showMenu by remember { mutableStateOf(false) }

    val callManager = remember { CallManager(context) }
    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }

    // Screen Share Launcher
    val screenShareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            callManager.startScreenShare(result.data!!)
            showMenu = false
        }
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val proximityWakeLock = remember { powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "NeoConnect::ProximityLock") }

    DisposableEffect(Unit) {
        if (isVideoCall) {
            (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (!proximityWakeLock.isHeld) proximityWakeLock.acquire()
        }
        onDispose {
            (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (proximityWakeLock.isHeld) proximityWakeLock.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) { startCall(callManager, localView, remoteView, roomId, isVideoCall) }
    }

    LaunchedEffect(Unit) {
        callManager.onRemoteStream = { track -> Handler(Looper.getMainLooper()).post { try { track.addSink(remoteView); isRemoteVideoAdded = true } catch (e: Exception) {} } }
        callManager.onCallEnded = { Handler(Looper.getMainLooper()).post { onCallEnd() } }
        callManager.onConnected = { Handler(Looper.getMainLooper()).post { isCallConnected = true } }
        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            startCall(callManager, localView, remoteView, roomId, isVideoCall)
        } else {
            permissionLauncher.launch(perms)
        }
    }

    LaunchedEffect(isCallConnected) { if (isCallConnected) while (true) { delay(1000L); callDuration++ } }

    // Main Layout
    Box(modifier = Modifier.fillMaxSize()) {
        
        if (!isVideoCall || !isRemoteVideoAdded) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }
        
        if (isVideoCall) {
            AndroidView(factory = { remoteView }, modifier = Modifier.fillMaxSize())
            if (!isRemoteVideoAdded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Cyan)
            }
            AndroidView(
                factory = { localView },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { callManager.switchCamera() }
            )
        }

        if (!isVideoCall) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Person, "Audio", Modifier.size(120.dp), Color.White)
                Spacer(Modifier.height(16.dp))
                Text(formatDuration(callDuration), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isVideoCall && callDuration > 0L) {
            Text(
                formatDuration(callDuration),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }

        // --- Animated Bottom Menu (Slide Up from Bottom) ---
        AnimatedVisibility(
            visible = showMenu && isVideoCall,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MenuContent(
                onDismiss = { showMenu = false },
                onShareScreenClicked = {
                    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            )
        }

        // Controls
        ControlBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            isMuted = isMuted,
            isVideoOff = isVideoOff,
            isSpeakerOn = isSpeakerOn,
            isVideoCall = isVideoCall,
            onMuteToggle = { isMuted = !isMuted; callManager.toggleMute(isMuted) },
            onVideoToggle = { isVideoOff = !isVideoOff; callManager.toggleCamera(isVideoOff) },
            onSpeakerToggle = { isSpeakerOn = !isSpeakerOn; callManager.enableSpeaker(isSpeakerOn) },
            onEndCall = { callManager.endCall(); onCallEnd() },
            onMenuClicked = { showMenu = !showMenu },
            showMenuButton = isVideoCall
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try { localView.release() } catch (e: Exception) {}
            try { remoteView.release() } catch (e: Exception) {}
            callManager.endCall()
        }
    }
}

@Composable
fun MenuContent(onDismiss: () -> Unit, onShareScreenClicked: () -> Unit) {
    val menuItems = listOf(
        MenuItem("Participants", Icons.Default.People, Color.White),
        MenuItem("Share Screen", Icons.Default.ScreenShare, Color(0xFF00BCD4)),
        MenuItem("Record", Icons.Default.FiberManualRecord, Color.Red),
        MenuItem("Background", Icons.Default.Landscape, Color.White),
        MenuItem("Subtitles", Icons.Default.Subtitles, Color.White),
        MenuItem("Transcript", Icons.Default.Description, Color.White)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE6171717), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("More Options", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).clickable { onDismiss() }
            )
        }

        // Grid Menu
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(menuItems) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (item.title == "Share Screen") onShareScreenClicked()
                        else onDismiss()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.title, tint = item.tint, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.title, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

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
    onMenuClicked: () -> Unit,
    showMenuButton: Boolean
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            background = if (isMuted) Color.Red else Color.DarkGray,
            onClick = onMuteToggle
        )
        ControlButton(
            icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
            background = if (isSpeakerOn) Color(0xFF4CAF50) else Color.DarkGray,
            onClick = onSpeakerToggle
        )

        if (isVideoCall) {
            ControlButton(
                icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                background = if (isVideoOff) Color.Red else Color.DarkGray,
                onClick = onVideoToggle
            )
        }

        if (showMenuButton) {
            ControlButton(
                icon = Icons.Default.MoreHoriz,
                background = Color.DarkGray,
                onClick = onMenuClicked
            )
        }

        ControlButton(
            icon = Icons.Default.CallEnd,
            background = Color.Red,
            onClick = onEndCall
        )
    }
}

@Composable
fun ControlButton(icon: ImageVector, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

private fun startCall(
    manager: CallManager,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer,
    roomId: String,
    isVideoCall: Boolean
) {
    manager.init()
    val eglContext = manager.eglBase?.eglBaseContext ?: return
    try {
        remoteView.init(eglContext, null)
        remoteView.setZOrderMediaOverlay(false)
        remoteView.setMirror(false)
    } catch (e: Exception) {}
    
    manager.createStream(isVideoCall)
    
    if (isVideoCall) {
        try {
            localView.init(eglContext, null)
            localView.setZOrderMediaOverlay(true)
            localView.setMirror(true)
        } catch (e: Exception) {}
        manager.startLocalVideoCapture(localView)
    }
    
    manager.joinRoom(roomId)
}

fun formatDuration(seconds: Long): String {
    val mins = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds - TimeUnit.MINUTES.toSeconds(mins)
    return String.format("%02d:%02d", mins, secs)
}
