package com.neoconnect.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neoconnect.app.webrtc.CallManager
import com.neoconnect.app.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    roomId: String,
    onEnd: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    
    val manager = remember {
        CallManager()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video View
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { renderer ->
                    manager.init()
                    manager.startLocalStream(context, renderer)
                    manager.joinRoom(roomId)
                    
                    manager.onRemoteStream = { track ->
                        track.addSink(renderer)
                    }
                    manager.onCallEnded = { onEnd() }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Local Video View
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { renderer ->
                    manager.startLocalStream(renderer)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(120.dp, 160.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // Control Bar
        ControlBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            isMuted = isMuted,
            isVideoOff = isVideoOff,
            onMuteToggle = {
                isMuted = !isMuted
                manager.toggleMute(isMuted)
            },
            onVideoToggle = {
                isVideoOff = !isVideoOff
                manager.toggleCamera(isVideoOff)
            },
            onEndCall = {
                manager.endCall()
                onEnd()
            }
        )
    }
}

@Composable
fun ControlBar(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    isVideoOff: Boolean,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            background = if (isMuted) Color.Red else Color.DarkGray,
            onClick = onMuteToggle
        )
        ControlButton(
            icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
            background = if (isVideoOff) Color.Red else Color.DarkGray,
            onClick = onVideoToggle
        )
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
            .size(56.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}