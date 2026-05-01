package com.neoconnect.app.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neoconnect.app.webrtc.CallManager
import com.neoconnect.app.ui.theme.NeoPrimary

@Composable
fun CallScreen(
    roomId: String,
    onEnd: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    
    val manager = remember {
        CallManager().apply {
            joinRoom(roomId)
            onRemoteStream = { _ -> }
            onCallEnded = { onEnd() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video View
        AndroidView(
            factory = { context ->
                SurfaceView(context).also {
                    manager.init(it, context)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Local Video View
        AndroidView(
            factory = { context ->
                SurfaceView(context).also {
                    manager.initLocal(it)
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
            isActive = isMuted,
            onClick = onMuteToggle
        )
        ControlButton(
            isActive = isVideoOff,
            onClick = onVideoToggle
        )
        ControlButton(
            isEndCall = true,
            onClick = onEndCall
        )
    }
}

@Composable
fun ControlButton(
    isActive: Boolean = false,
    isEndCall: Boolean = false,
    onClick: () -> Unit
) {
    val background = when {
        isEndCall -> Color.Red
        isActive -> Color.Red
        else -> Color.DarkGray
    }
    
    val contentDescription = when {
        isEndCall -> "End Call"
        isActive -> "Unmute"
        else -> "Mute"
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isEndCall -> "X"
                isActive -> "X"
                else -> "O"
            },
            color = Color.White
        )
    }
}
