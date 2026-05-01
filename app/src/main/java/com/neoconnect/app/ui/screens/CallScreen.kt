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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.neoconnect.app.webrtc.CallManager
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.concurrent.TimeUnit

@Composable
fun CallScreen(
    roomId: String,
    isVideoCall: Boolean,
    onCallEnd: () -> Unit
) {
    val context = LocalContext.current
    
    // States
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isRemoteVideoAdded by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    
    var isCallConnected by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0L) }

    // Manager
    val callManager = remember { CallManager(context) }

    // Views
    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }

    // --- Proximity & Screen Logic ---
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val proximityWakeLock = remember {
        powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "NeoConnect::ProximityLock"
        )
    }

    DisposableEffect(Unit) {
        if (isVideoCall) {
            (context as? android.app.Activity)?.window?.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } else {
            if (!proximityWakeLock.isHeld) proximityWakeLock.acquire()
        }
        
        onDispose {
            (context as? android.app.Activity)?.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            if (proximityWakeLock.isHeld) proximityWakeLock.release()
        }
    }
    // -------------------------------

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            startCall(callManager, localView, remoteView, roomId, isVideoCall)
        }
    }

    // Main Logic
    LaunchedEffect(Unit) {
        callManager.onRemoteStream = { track: VideoTrack ->
            Handler(Looper.getMainLooper()).post {
                remoteView.clearImage()
                track.addSink(remoteView)
                isRemoteVideoAdded = true
            }
        }
        callManager.onCallEnded = {
            Handler(Looper.getMainLooper()).post {
                onCallEnd()
            }
        }
        callManager.onConnected = {
            Handler(Looper.getMainLooper()).post {
                isCallConnected = true
            }
        }

        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            startCall(callManager, localView, remoteView, roomId, isVideoCall)
        } else {
            permissionLauncher.launch(perms)
        }
    }

    // Timer
    LaunchedEffect(isCallConnected) {
        if (isCallConnected) {
            while (true) {
                delay(1000L)
                callDuration++
            }
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isVideoCall || !isRemoteVideoAdded) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        if (isVideoCall) {
            AndroidView(factory = { remoteView }, modifier = Modifier.fillMaxSize())
            if (!isRemoteVideoAdded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            }
        }

        if (isVideoCall) {
            AndroidView(
                factory = { localView },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(120.dp, 180.dp)
                    .clip(RoundedCornerShape(16.dp)).clickable { callManager.switchCamera() }
            )
        }

        if (!isVideoCall) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Person, "Audio", Modifier.size(120.dp), Color.White)
                Spacer(Modifier.height(16.dp))
                Text(formatDuration(callDuration), color = Color.White, fontSize = 24.sp)
            }
        }

        if (isVideoCall && callDuration > 0L) {
            Text(formatDuration(callDuration), color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp))
        }

        ControlBar(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            isMuted = isMuted, isVideoOff = isVideoOff, isSpeakerOn = isSpeakerOn, isVideoCall = isVideoCall,
            onMuteToggle = { isMuted = !isMuted; callManager.toggleMute(isMuted) },
            onVideoToggle = { isVideoOff = !isVideoOff; callManager.toggleCamera(isVideoOff) },
            onSpeakerToggle = { isSpeakerOn = !isSpeakerOn; callManager.enableSpeaker(isSpeakerOn) },
            onEndCall = { callManager.endCall(); onCallEnd() }
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

// Updated startCall function with try-catch
private fun startCall(
    manager: CallManager,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer,
    roomId: String,
    isVideoCall: Boolean
) {
    manager.init()
    val eglContext = manager.eglBase?.eglBaseContext ?: return

    // Already initialized হলে আবার init করবে না বা Error হলে Catch করবে
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

@Composable
fun ControlBar(modifier: Modifier, isMuted: Boolean, isVideoOff: Boolean, isSpeakerOn: Boolean, isVideoCall: Boolean, onMuteToggle: () -> Unit, onVideoToggle: () -> Unit, onSpeakerToggle: () -> Unit, onEndCall: () -> Unit) {
    Row(modifier = modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, background = if (isMuted) Color.Red else Color.DarkGray, onClick = onMuteToggle)
        ControlButton(icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown, background = if (isSpeakerOn) Color(0xFF4CAF50) else Color.DarkGray, onClick = onSpeakerToggle)
        if (isVideoCall) { ControlButton(icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam, background = if (isVideoOff) Color.Red else Color.DarkGray, onClick = onVideoToggle) }
        ControlButton(icon = Icons.Default.CallEnd, background = Color.Red, onClick = onEndCall)
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(background).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}