package com.neoconnect.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LinkedCamera
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onJoinCall: (String, Boolean) -> Unit) {
    var roomId by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    // ── Animated floating orbs ──────────────────────────────────
    val orb1X by infiniteTransition.animateFloat(
        initialValue = -0.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Reverse),
        label = "orb1x"
    )
    val orb1Y by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "orb1y"
    )
    val orb2X by infiniteTransition.animateFloat(
        initialValue = 1.2f, targetValue = -0.2f,
        animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Reverse),
        label = "orb2x"
    )
    val orb2Y by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(11000), RepeatMode.Reverse),
        label = "orb2y"
    )
    val orb3X by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "orb3x"
    )
    val orb3Y by infiniteTransition.animateFloat(
        initialValue = -0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(13000), RepeatMode.Reverse),
        label = "orb3y"
    )

    // ── Pulse animation for icon ────────────────────────────────
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    // ── Shimmer offset ──────────────────────────────────────────
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -400f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Restart),
        label = "shimmer"
    )

    // ── Glow rotation ───────────────────────────────────────────
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Restart),
        label = "glow"
    )

    // ── Color palette ───────────────────────────────────────────
    val deepBlack = Color(0xFF06080D)
    val surfaceDark = Color(0xFF0D1117)
    val accentViolet = Color(0xFF8B5CF6)
    val accentCyan = Color(0xFF22D3EE)
    val accentPink = Color(0xFFF472B6)
    val accentGreen = Color(0xFF34D399)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)

    val bgGradient = Brush.verticalGradient(
        colors = listOf(deepBlack, surfaceDark, Color(0xFF0B1222))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        // ── Floating background orbs ─────────────────────────────
        Box(
            modifier = Modifier
                .offset(orb1X.dp * 350, orb1Y.dp * 700)
                .size(180.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentViolet.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .offset(orb2X.dp * 350, orb2Y.dp * 700)
                .size(200.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentCyan.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .offset(orb3X.dp * 300, orb3Y.dp * 650)
                .size(160.dp)
                .blur(55.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentPink.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // ── Main content ─────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ── Logo / Title area ────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pulsing icon logo
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .shadow(24.dp, CircleShape, ambientColor = accentCyan.copy(alpha = 0.4f))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentViolet, accentCyan),
                                    start = Offset.Zero,
                                    end = Offset(72f, 72f)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "NeoConnect",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Next-Gen Video Calls",
                        color = accentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Glass card ───────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, 200)) + scaleIn(tween(600, 200), initialScale = 0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Color(0xFF111827).copy(alpha = 0.75f)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentViolet.copy(alpha = 0.3f),
                                    accentCyan.copy(alpha = 0.15f),
                                    accentPink.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Shimmer header bar ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = shimmerOffset.dp)
                                .width(120.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            accentCyan.copy(alpha = 0.7f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Enter a Room ID to connect",
                        color = textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Input field ────────────────────────────────
                    OutlinedTextField(
                        value = roomId,
                        onValueChange = { roomId = it },
                        label = { Text("Room ID") },
                        placeholder = { Text("e.g. 123456", color = Color(0xFF475569)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = accentViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = textPrimary,
                            cursorColor = accentCyan,
                            focusedBorderColor = accentViolet,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = accentCyan,
                            unfocusedLabelColor = textSecondary,
                            containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Video call button ──────────────────────────
                    val videoBtnGrad = Brush.horizontalGradient(
                        colors = listOf(accentViolet, Color(0xFF7C3AED), accentPink.copy(alpha = 0.7f))
                    )

                    Button(
                        onClick = { if (roomId.isNotBlank()) onJoinCall(roomId, true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = accentViolet.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(videoBtnGrad, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Join Video Call",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Audio call button ──────────────────────────
                    val audioBtnGrad = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0891B2), accentCyan, accentGreen.copy(alpha = 0.6f))
                    )

                    Button(
                        onClick = { if (roomId.isNotBlank()) onJoinCall(roomId, false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = accentCyan.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(audioBtnGrad, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Join Audio Call",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Helper text ──────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1200, 600))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkedCamera,
                        contentDescription = null,
                        tint = accentGreen.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use the same Room ID on another device to connect",
                        color = textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Animated signal bars decoration ───────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1400, 800))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(24.dp)
                ) {
                    repeat(5) { index ->
                        val barHeight by infiniteTransition.animateFloat(
                            initialValue = 6f + index * 3f,
                            targetValue = 18f + index * 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800 + index * 200, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(accentCyan, accentViolet)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
