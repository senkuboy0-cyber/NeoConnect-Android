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

data class VideoFilter(val name: String, val colorCode: String)

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
    var selectedFilter by remember { mutableStateOf("Normal") }
    
    val filters = listOf(
        VideoFilter("Normal", "#FFFFFF"),
        VideoFilter("Cool", "#BBDEFB"),
        VideoFilter("Warm", "#FFE0B2"),
        VideoFilter("B&W", "#9E9E9E"),
        VideoFilter("Vintage", "#D7CCC8")
    )

    val callManager = remember { CallManager(context) }
    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val proximityWakeLock = remember { powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "NeoConnect::ProximityLock") }

    DisposableEffect(Unit) {
        if (isVideoCall) { (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } 
        else { if (!proximityWakeLock.isHeld) proximityWakeLock.acquire() }
        onDispose {
            (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) { startCall(callManager, localView, remoteView, roomId, isVideoCall) } 
        else { permissionLauncher.launch(perms) }
    }

    LaunchedEffect(isCallConnected) { if (isCallConnected) while (true) { delay(1000L); callDuration++ } }

    // Main Layout
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Background & Video Views
        if (!isVideoCall || !isRemoteVideoAdded) { Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) }
        
        if (isVideoCall) {
            AndroidView(factory = { remoteView }, modifier = Modifier.fillMaxSize())
            if (!isRemoteVideoAdded) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary) }
            AndroidView(factory = { localView }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(120.dp, 180.dp).clip(RoundedCornerShape(16.dp)).clickable { callManager.switchCamera() })
        }

        // Audio UI
        if (!isVideoCall) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Person, "Audio", Modifier.size(120.dp), Color.White)
                Spacer(Modifier.height(16.dp))
                Text(formatDuration(callDuration), color = Color.White, fontSize = 24.sp)
            }
        }

        // Timer
        if (isVideoCall && callDuration > 0L) {
            Text(formatDuration(callDuration), color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }

        // 2. Animated Menu
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 130.dp)
        ) {
            AnimatedVisibility(
                visible = showMenu && isVideoCall,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { showMenu = false })
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Video Filters", color = Color.Cyan, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filters) { filter ->
                            FilterItem(filter = filter, isSelected = selectedFilter == filter.name) {
                                selectedFilter = filter.name
                            }
                        }
                    }
                }
            }
        }

        // 3. Controls
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
fun FilterItem(filter: VideoFilter, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(filter.colorCode)))
                .then(if (isSelected) Modifier.border(3.dp, Color.Cyan, CircleShape) else Modifier)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(filter.name, color = Color.White, fontSize = 11.sp)
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
        modifier = modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 12.dp), 
        horizontalArrangement = Arrangement.spacedBy(12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, background = if (isMuted) Color.Red else Color.DarkGray, onClick = onMuteToggle)
        ControlButton(icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown, background = if (isSpeakerOn) Color(0xFF4CAF50) else Color.DarkGray, onClick = onSpeakerToggle)
        
        if (isVideoCall) { 
            ControlButton(icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam, background = if (isVideoOff) Color.Red else Color.DarkGray, onClick = onVideoToggle) 
        }

        if (showMenuButton) {
            ControlButton(
                icon = Icons.Default.MoreHoriz,
                background = Color.DarkGray, 
                onClick = onMenuClicked
            )
        }
        
        ControlButton(icon = Icons.Default.CallEnd, background = Color.Red, onClick = onEndCall)
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(background).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

private fun startCall(manager: CallManager, localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, roomId: String, isVideoCall: Boolean) {
    manager.init()
    val eglContext = manager.eglBase?.eglBaseContext ?: return
    try { remoteView.init(eglContext, null); remoteView.setZOrderMediaOverlay(false); remoteView.setMirror(false) } catch (e: Exception) {}
    manager.createStream(isVideoCall)
    if (isVideoCall) { try { localView.init(eglContext, null); localView.setZOrderMediaOverlay(true); localView.setMirror(true) } catch (e: Exception) {}; manager.startLocalVideoCapture(localView) }
    manager.joinRoom(roomId)
}

fun formatDuration(seconds: Long): String {
    val mins = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds - TimeUnit.MINUTES.toSeconds(mins)
    return String.format("%02d:%02d", mins, secs)
}