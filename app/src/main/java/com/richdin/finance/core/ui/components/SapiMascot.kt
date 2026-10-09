package com.richdin.finance.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richdin.finance.core.model.CowMood
import com.richdin.finance.core.model.EarlyWarningStatus
import com.richdin.finance.core.ui.theme.*

@Composable
fun SapiMascotCard(
    mood: CowMood,
    status: EarlyWarningStatus,
    speechBubbleText: String,
    modifier: Modifier = Modifier
) {
    val (statusColor, containerColor, textColor) = when (status) {
        EarlyWarningStatus.SAFE -> Triple(FarmGreenPrimary, FarmGreenContainer, FarmGreenText)
        EarlyWarningStatus.WARNING -> Triple(FarmStrawWarning, FarmStrawContainer, FarmStrawText)
        EarlyWarningStatus.DANGER -> Triple(FarmBarnRed, FarmBarnRedContainer, FarmBarnRedText)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sapi mascot graphic
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            SapiMascot(mood = mood, size = 68.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Speech Bubble / Advice
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (status) {
                        EarlyWarningStatus.SAFE -> "Status: Aman 🟢"
                        EarlyWarningStatus.WARNING -> "Status: Waspada 🟡"
                        EarlyWarningStatus.DANGER -> "Status: Bahaya 🔴"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = speechBubbleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
fun SapiMascot(
    mood: CowMood,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    // Subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "CowAnim")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bounce"
    )

    val tearAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Tears"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .offset(y = bounceOffset.dp)
    ) {
        val w = this.size.width
        val h = this.size.height

        val cowWhite = Color(0xFFFBFBFB)
        val cowSpot = Color(0xFF37474F)
        val cowPink = Color(0xFFFFB6C1)
        val cowHorn = Color(0xFFFFD54F)
        val eyeColor = Color(0xFF212121)
        val tearColor = Color(0xFF29B6F6)

        // 1. Horns
        val hornPathL = Path().apply {
            moveTo(w * 0.28f, h * 0.28f)
            quadraticTo(w * 0.18f, h * 0.12f, w * 0.26f, h * 0.10f)
            quadraticTo(w * 0.32f, h * 0.18f, w * 0.35f, h * 0.28f)
            close()
        }
        val hornPathR = Path().apply {
            moveTo(w * 0.72f, h * 0.28f)
            quadraticTo(w * 0.82f, h * 0.12f, w * 0.74f, h * 0.10f)
            quadraticTo(w * 0.68f, h * 0.18f, w * 0.65f, h * 0.28f)
            close()
        }
        drawPath(hornPathL, cowHorn)
        drawPath(hornPathR, cowHorn)

        // 2. Ears
        drawOval(
            color = cowWhite,
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = Size(w * 0.24f, h * 0.16f)
        )
        drawOval(
            color = cowPink,
            topLeft = Offset(w * 0.12f, h * 0.29f),
            size = Size(w * 0.16f, h * 0.10f)
        )
        drawOval(
            color = cowWhite,
            topLeft = Offset(w * 0.68f, h * 0.26f),
            size = Size(w * 0.24f, h * 0.16f)
        )
        drawOval(
            color = cowPink,
            topLeft = Offset(w * 0.72f, h * 0.29f),
            size = Size(w * 0.16f, h * 0.10f)
        )

        // 3. Head Base
        drawRoundRect(
            color = cowWhite,
            topLeft = Offset(w * 0.20f, h * 0.22f),
            size = Size(w * 0.60f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )

        // 4. Cow Spots
        drawOval(
            color = cowSpot,
            topLeft = Offset(w * 0.22f, h * 0.24f),
            size = Size(w * 0.20f, h * 0.20f)
        )
        drawOval(
            color = cowSpot,
            topLeft = Offset(w * 0.62f, h * 0.30f),
            size = Size(w * 0.14f, h * 0.14f)
        )

        // 5. Snout / Muzzle
        drawRoundRect(
            color = cowPink,
            topLeft = Offset(w * 0.25f, h * 0.52f),
            size = Size(w * 0.50f, h * 0.30f),
            cornerRadius = CornerRadius(w * 0.15f, h * 0.15f)
        )
        // Nostrils
        drawCircle(color = cowSpot, radius = w * 0.035f, center = Offset(w * 0.40f, h * 0.62f))
        drawCircle(color = cowSpot, radius = w * 0.035f, center = Offset(w * 0.60f, h * 0.62f))

        // 6. Eyes & Expression according to CowMood
        when (mood) {
            CowMood.HAPPY -> {
                // Cheerful smiling curved eyes ^ ^
                val eyeL = Path().apply {
                    moveTo(w * 0.33f, h * 0.42f)
                    quadraticTo(w * 0.38f, h * 0.35f, w * 0.43f, h * 0.42f)
                }
                val eyeR = Path().apply {
                    moveTo(w * 0.57f, h * 0.42f)
                    quadraticTo(w * 0.62f, h * 0.35f, w * 0.67f, h * 0.42f)
                }
                drawPath(eyeL, eyeColor, style = Stroke(width = w * 0.035f))
                drawPath(eyeR, eyeColor, style = Stroke(width = w * 0.035f))

                // Big happy smile
                val smile = Path().apply {
                    moveTo(w * 0.40f, h * 0.72f)
                    quadraticTo(w * 0.50f, h * 0.79f, w * 0.60f, h * 0.72f)
                }
                drawPath(smile, eyeColor, style = Stroke(width = w * 0.03f))

                // Rosy cheeks
                drawCircle(color = Color(0xFFFF80AB).copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(w * 0.25f, h * 0.48f))
                drawCircle(color = Color(0xFFFF80AB).copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(w * 0.75f, h * 0.48f))
            }
            CowMood.NEUTRAL -> {
                // Neutral watchful dot eyes
                drawCircle(color = eyeColor, radius = w * 0.045f, center = Offset(w * 0.38f, h * 0.40f))
                drawCircle(color = Color.White, radius = w * 0.015f, center = Offset(w * 0.37f, h * 0.39f))
                drawCircle(color = eyeColor, radius = w * 0.045f, center = Offset(w * 0.62f, h * 0.40f))
                drawCircle(color = Color.White, radius = w * 0.015f, center = Offset(w * 0.61f, h * 0.39f))

                // Flat calm mouth
                drawLine(
                    color = eyeColor,
                    start = Offset(w * 0.44f, h * 0.73f),
                    end = Offset(w * 0.56f, h * 0.73f),
                    strokeWidth = w * 0.03f
                )
            }
            CowMood.SAD -> {
                // Worried downward angled eyes
                val eyeL = Path().apply {
                    moveTo(w * 0.33f, h * 0.38f)
                    quadraticTo(w * 0.38f, h * 0.44f, w * 0.43f, h * 0.40f)
                }
                val eyeR = Path().apply {
                    moveTo(w * 0.57f, h * 0.40f)
                    quadraticTo(w * 0.62f, h * 0.44f, w * 0.67f, h * 0.38f)
                }
                drawPath(eyeL, eyeColor, style = Stroke(width = w * 0.035f))
                drawPath(eyeR, eyeColor, style = Stroke(width = w * 0.035f))

                // Frown mouth
                val frown = Path().apply {
                    moveTo(w * 0.42f, h * 0.76f)
                    quadraticTo(w * 0.50f, h * 0.69f, w * 0.58f, h * 0.76f)
                }
                drawPath(frown, eyeColor, style = Stroke(width = w * 0.03f))

                // Teardrops falling
                val tearY = h * 0.45f + (tearAnim * h * 0.25f)
                drawOval(
                    color = tearColor,
                    topLeft = Offset(w * 0.31f, tearY),
                    size = Size(w * 0.05f, h * 0.09f)
                )
                drawOval(
                    color = tearColor,
                    topLeft = Offset(w * 0.65f, tearY),
                    size = Size(w * 0.05f, h * 0.09f)
                )
            }
        }
    }
}
