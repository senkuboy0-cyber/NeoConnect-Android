package com.neoconnect.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.ArrowForward
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
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onJoinCall: (String, Boolean) -> Unit) {
    var roomId by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    
    // Spring animation for interactive elements
    val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 100f)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    // ── Animated floating orbs with smoother circular motion ──────────────────────────────────
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000), RepeatMode.Restart),
        label = "time"
    )
    
    val orb1X = -0.2f + sin(time) * 0.3f + cos(time * 0.7f) * 0.2f
    val orb1Y = 0.3f + cos(time * 1.2f) * 0.25f + sin(time * 0.5f) * 0.15f
    
    val orb2X = 1.2f + cos(time * 0.8f) * 0.35f + sin(time * 1.3f) * 0.2f
    val orb2Y = 0.6f + sin(time * 0.9f) * 0.3f + cos(time * 0.6f) * 0.2f
    
    val orb3X = 0.5f + sin(time * 1.1f) * 0.25f + cos(time * 0.4f) * 0.15f
    val orb3Y = -0.1f + cos(time * 0.7f) * 0.35f + sin(time * 0.8f) * 0.2f

    // ── Pulse animation for icon with spring ────────────────────────────────
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulse"
    )

    // ── Shimmer offset ──────────────────────────────────────────
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -400f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseOutCubic), RepeatMode.Restart),
        label = "shimmer"
    )

    // ── Glow rotation ───────────────────────────────────────────
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000), RepeatMode.Restart),
        label = "glow"
    )
    
    // ── Floating particles ──────────────────────────────────────
    val particles = remember { List(8) { index -> 
        Triple(
            infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8000 + index * 500), RepeatMode.Restart),
                label = "particle$index"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1500 + index * 200), RepeatMode.Reverse),
                label = "particleAlpha$index"
            ),
            index
        )
    }}

    // ── Enhanced Color palette ───────────────────────────────────────────
    val deepBlack = Color(0xFF030712)
    val surfaceDark = Color(0xFF0F172A)
    val accentViolet = Color(0xFF8B5CF6)
    val accentCyan = Color(0xFF22D3EE)
    val accentPink = Color(0xFFF472B6)
    val accentGreen = Color(0xFF34D399)
    val accentOrange = Color(0xFFF59E0B)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)
    
    val bgGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF020617),
            deepBlack
        ),
        center = Offset(0.5f, 0.3f),
        radius = 1.2f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        // ── Floating particles decoration ─────────────────────────
        particles.forEach { (angleAnim, alphaAnim, index) ->
            val angleRad = angleAnim.value * Math.PI.toFloat() / 180f
            val radius = 150 + index * 25
            val px = (Math.cos(angleRad.toDouble()) * radius).toFloat()
            val py = (Math.sin(angleRad.toDouble()) * radius).toFloat()
            
            Box(
                modifier = Modifier
                    .offset(px.dp, py.dp)
                    .size((4 + index % 3 * 2).dp)
                    .graphicsLayer { 
                        alpha = alphaAnim.value * 0.5f
                        rotationZ = angleAnim.value
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                listOf(accentViolet, accentCyan, accentPink, accentGreen)[index % 4].copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }
        
        // ── Enhanced floating background orbs with rotation ─────────────────────────────
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = glowRotation }
                .offset(orb1X.dp * 350, orb1Y.dp * 700)
                .size(200.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentViolet.copy(alpha = 0.3f), accentPink.copy(alpha = 0.1f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = -glowRotation * 0.7f }
                .offset(orb2X.dp * 350, orb2Y.dp * 700)
                .size(220.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentCyan.copy(alpha = 0.25f), accentGreen.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = glowRotation * 0.5f }
                .offset(orb3X.dp * 300, orb3Y.dp * 650)
                .size(180.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentPink.copy(alpha = 0.22f), accentOrange.copy(alpha = 0.08f), Color.Transparent)
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
            Spacer(modifier = Modifier.height(32.dp))

            // ── Enhanced Logo / Title area with animations ────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { -it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pulsing icon logo with enhanced glow and rotation
                    Box(
                        modifier = Modifier
                            .graphicsLayer { 
                                rotationZ = glowRotation * 0.1f
                                shadowElevation = 32f
                                ambientShadowColor = accentViolet.copy(alpha = 0.5f)
                                spotShadowColor = accentCyan.copy(alpha = 0.6f)
                            }
                            .size(80.dp)
                            .scale(pulseScale)
                            .shadow(32.dp, CircleShape, ambientColor = accentViolet.copy(alpha = 0.5f), spotColor = accentCyan.copy(alpha = 0.6f))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentViolet, accentPink, accentCyan),
                                    start = Offset.Zero,
                                    end = Offset(80f, 80f)
                                ),
                                CircleShape
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = -glowRotation * 0.1f }
                        )
                        
                        // Inner glow ring
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.radialGradient(
                                        colors = listOf(accentCyan.copy(alpha = 0.4f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main title with gradient text effect simulation
                    Text(
                        text = "NeoConnect",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        letterSpacing = (-1).sp,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 0.95f
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle with animated shimmer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentCyan.copy(alpha = 0.1f),
                                        accentViolet.copy(alpha = 0.1f),
                                        accentPink.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨ Next-Gen Video Calls ✨",
                            color = accentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Enhanced Glass card with animations ───────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1200, 300)) + scaleIn(tween(800, 300), initialScale = 0.88f)
            ) {
                var buttonHovered by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B).copy(alpha = 0.85f),
                                    Color(0xFF0F172A).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentViolet.copy(alpha = 0.4f),
                                    accentCyan.copy(alpha = 0.2f),
                                    accentPink.copy(alpha = 0.4f),
                                    accentGreen.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = accentViolet.copy(alpha = 0.15f),
                            spotColor = accentCyan.copy(alpha = 0.2f)
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Animated shimmer header bar ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentViolet.copy(alpha = 0.2f),
                                        accentCyan.copy(alpha = 0.2f),
                                        accentPink.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = shimmerOffset.dp)
                                .width(140.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            accentCyan.copy(alpha = 0.9f),
                                            accentViolet.copy(alpha = 0.7f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = accentViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Enter a Room ID to connect",
                            color = textPrimary.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Enhanced Input field with focus animation ────────────────────────────────
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFieldFocused by interactionSource.collectIsFocusedAsState()
                    
                    OutlinedTextField(
                        value = roomId,
                        onValueChange = { roomId = it },
                        label = { 
                            AnimatedContent(
                                targetState = if (roomId.isEmpty()) "Room ID" else "Connected to",
                                transitionSpec = {
                                    slideInVertically { it } + fadeIn() togetherWith
                                    slideOutVertically { -it } + fadeOut()
                                }
                            ) { label ->
                                Text(label)
                            }
                        },
                        placeholder = { 
                            Text(
                                "e.g. 123456", 
                                color = textSecondary.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            ) 
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(accentViolet.copy(alpha = 0.3f), accentPink.copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = accentViolet,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (roomId.isNotEmpty()) {
                                IconButton(
                                    onClick = { roomId = "" },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(textSecondary.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        Icons.Rounded.CallEnd,
                                        contentDescription = "Clear",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = textPrimary,
                            cursorColor = accentCyan,
                            focusedBorderColor = accentViolet.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = accentCyan,
                            unfocusedLabelColor = textSecondary,
                            containerColor = Color(0xFF0F172A).copy(alpha = 0.7f),
                            focusedLeadingIconColor = accentViolet,
                            unfocusedLeadingIconColor = accentViolet.copy(alpha = 0.7f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        interactionSource = interactionSource
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Enhanced Video call button with animations ──────────────────────────
                    val videoBtnGrad = Brush.linearGradient(
                        colors = listOf(accentViolet, Color(0xFF7C3AED), accentPink.copy(alpha = 0.8f)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                    
                    val videoBtnScale by animateFloatAsState(
                        targetValue = if (roomId.isNotBlank()) 1f else 0.95f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
                        label = "videoBtnScale"
                    )

                    Button(
                        onClick = { if (roomId.isNotBlank()) onJoinCall(roomId, true) },
                        enabled = roomId.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .scale(videoBtnScale)
                            .shadow(
                                elevation = if (roomId.isNotBlank()) 20.dp else 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = accentViolet.copy(alpha = if (roomId.isNotBlank()) 0.4f else 0.15f),
                                spotColor = accentPink.copy(alpha = if (roomId.isNotBlank()) 0.3f else 0.1f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (roomId.isNotBlank()) videoBtnGrad else Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF475569), Color(0xFF64748B))
                                    ),
                                    RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            if (roomId.isNotBlank()) rotationZ = glowRotation * 0.05f
                                        }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "🎥 Join Video Call",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Enhanced Audio call button with animations ──────────────────────────
                    val audioBtnGrad = Brush.linearGradient(
                        colors = listOf(Color(0xFF0891B2), accentCyan, accentGreen.copy(alpha = 0.7f)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                    
                    val audioBtnScale by animateFloatAsState(
                        targetValue = if (roomId.isNotBlank()) 1f else 0.96f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 140f),
                        label = "audioBtnScale"
                    )

                    Button(
                        onClick = { if (roomId.isNotBlank()) onJoinCall(roomId, false) },
                        enabled = roomId.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .scale(audioBtnScale)
                            .shadow(
                                elevation = if (roomId.isNotBlank()) 16.dp else 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = accentCyan.copy(alpha = if (roomId.isNotBlank()) 0.3f else 0.1f),
                                spotColor = accentGreen.copy(alpha = if (roomId.isNotBlank()) 0.25f else 0.08f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (roomId.isNotBlank()) audioBtnGrad else Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF475569), Color(0xFF64748B))
                                    ),
                                    RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            if (roomId.isNotBlank()) rotationZ = -glowRotation * 0.03f
                                        }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "📞 Join Audio Call",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Enhanced Helper text with animations ──────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1400, 700)) + slideInVertically(tween(1400, 700)) { it / 4 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentGreen.copy(alpha = 0.08f),
                                    accentCyan.copy(alpha = 0.05f),
                                    accentGreen.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Animated connection icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(accentGreen.copy(alpha = 0.3f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkedCamera,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer {
                                    rotationZ = glowRotation * 0.08f
                                    scaleX = 1f + sin(time) * 0.05f
                                    scaleY = 1f + sin(time) * 0.05f
                                }
                        )
                    }
                    Text(
                        text = "Use the same Room ID on another device to connect instantly",
                        color = textPrimary.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Enhanced Animated signal bars decoration ───────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1600, 900)) + scaleIn(tween(1000, 900), initialScale = 0.5f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentViolet.copy(alpha = 0.1f),
                                    accentCyan.copy(alpha = 0.1f),
                                    accentPink.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    repeat(5) { index ->
                        val barHeight by infiniteTransition.animateFloat(
                            initialValue = 8f + index * 2f,
                            targetValue = 16f + (4 - index) * 2f + sin(time * (1 + index * 0.2f)) * 6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900 + index * 150, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar$index"
                        )
                        val barColor = when (index) {
                            0 -> accentCyan
                            1 -> accentViolet
                            2 -> accentPink
                            3 -> accentGreen
                            else -> accentOrange
                        }
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            barColor.copy(alpha = 0.4f),
                                            barColor,
                                            barColor.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(3.dp),
                                    ambientColor = barColor.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
