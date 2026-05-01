package com.neoconnect.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    isVideoCall: Boolean = true,
    onCallEnd: () -> Unit
) {
    val context = LocalContext.current
    
    // State
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isRemoteVideoAdded by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) } // Video hole true, Audio hole false
    var callDuration by remember { mutableStateOf(0L) }
    
    // Views
    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }
    
    // Manager
    val callManager = remember { CallManager(context) }

    // Timer Logic
    LaunchedEffect(key1 = callManager.onConnected) {
        if (isRemoteVideoAdded || true) { // Start timer when connected
            while (true) {
                delay(1000L)
                callDuration++
            }
        }
    }

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            initializeAndStartCall(callManager, localView, remoteView, roomId, isVideoCall, onCallEnd) { 
                isRemoteVideoAdded = true 
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            initializeAndStartCall(callManager, localView, remoteView, roomId, isVideoCall, onCallEnd) { 
                isRemoteVideoAdded = true 
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Color
        if (!isVideoCall) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        // Remote Video (Full Screen)
        if (isVideoCall) {
            if (isRemoteVideoAdded) {
                AndroidView(
                    factory = { remoteView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Local Video (PiP)
            AndroidView(
                factory = { localView },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { callManager.switchCamera() }
            )
        } else {
            // Audio Call UI
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Audio Call",
                    modifier = Modifier.size(120.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatDuration(callDuration),
                    color = Color.White,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        // Timer for Video Call (Top Center)
        if (isVideoCall && callDuration > 0L) {
            Text(
                text = formatDuration(callDuration),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
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
            onMuteToggle = {
                isMuted = !isMuted
                callManager.toggleMute(isMuted)
            },
            onVideoToggle = {
                isVideoOff = !isVideoOff
                callManager.toggleCamera(isVideoOff)
            },
            onSpeakerToggle = {
                isSpeakerOn = !isSpeakerOn
                callManager.enableSpeaker(isSpeakerOn)
            },
            onEndCall = {
                callManager.endCall()
                onCallEnd()
            }
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            localView.release()
            remoteView.release()
            callManager.endCall()
        }
    }
}

private fun initializeAndStartCall(
    manager: CallManager,
    localView: SurfaceViewRenderer,
    remoteView: SurfaceViewRenderer,
    roomId: String,
    isVideoCall: Boolean,
    onEnd: () -> Unit,
    onRemote: () -> Unit
) {
    manager.init()
    manager.startLocalStream(localView, isVideoCall)
    manager.joinRoom(roomId)
    
    manager.onRemoteStream = { track ->
        track.addSink(remoteView)
        onRemote()
    }
    manager.onCallEnded = { onEnd() }
}

// Helper to format time
fun formatDuration(seconds: Long): String {
    val mins = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds - TimeUnit.MINUTES.toSeconds(mins)
    return String.format("%02d:%02d", mins, secs)
}

@Composable
fun ControlBar(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    isVideoOff: Boolean,
    isSpeakerOn: Boolean,
    isVideoCall: Boolean,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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

        ControlButton(
            icon = Icons.Default.CallEnd,
            background = Color.Red,
            onClick = onEndCall
        )
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
