package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.pixel.CharacterSpriteRenderer
import com.example.game.pixel.WorldTileAtlas
import com.example.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = FarmParchment,
    borderColor: Color = FarmWoodDark,
    innerBorderColor: Color = FarmWoodLight,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(3.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .border(1.5.dp, innerBorderColor.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FarmLeafGreen,
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "pixel_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "btnScale")

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.45f),
        contentColor = contentColor,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .border(2.5.dp, FarmWoodDark, RoundedCornerShape(10.dp))
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PixelCharacterPreview(
    appearance: CharacterAppearance,
    direction: Direction,
    action: CharacterAction,
    frame: Int,
    blink: Boolean,
    modifier: Modifier = Modifier
) {
    val sprite = remember(appearance, direction, action, frame, blink) {
        CharacterSpriteRenderer.getSprite(appearance, direction, action, frame, blink)
    }
    Canvas(modifier = modifier) {
        drawImage(
            image = sprite,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
fun PixelPortraitPreview(
    appearance: CharacterAppearance,
    expression: Expression? = null,
    blink: Boolean = false,
    modifier: Modifier = Modifier
) {
    val portrait = remember(appearance, expression, blink) {
        CharacterSpriteRenderer.getPortrait(appearance, expression, blink)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(2.5.dp, FarmWoodDark, RoundedCornerShape(10.dp))
            .background(FarmCreamGold)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = portrait,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun PixelItemIcon(
    item: InventoryItem,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val bmp = remember(item.id) { WorldTileAtlas.getItemIcon(item) }
    Canvas(modifier = modifier.size(size)) {
        drawImage(
            image = bmp,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
fun AffectionHeartsRow(
    affection: Int,
    modifier: Modifier = Modifier
) {
    val filledHearts = (affection / 20).coerceIn(0, 5)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { idx ->
            Icon(
                imageVector = if (idx < filledHearts) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (idx < filledHearts) FarmRose else FarmInkMuted.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$affection/100",
            style = MaterialTheme.typography.labelMedium,
            color = FarmInk
        )
    }
}

@Composable
fun VirtualJoystick(
    onVectorChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 76f

    Box(
        modifier = modifier
            .size(124.dp)
            .clip(CircleShape)
            .background(FarmWoodDark.copy(alpha = 0.48f))
            .border(2.5.dp, FarmParchment.copy(alpha = 0.72f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onVectorChanged(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val cand = knobOffset + dragAmount
                        val len = hypot(cand.x, cand.y)
                        knobOffset = if (len > maxRadiusPx) {
                            Offset(cand.x / len * maxRadiusPx, cand.y / len * maxRadiusPx)
                        } else cand
                        val normLen = hypot(knobOffset.x, knobOffset.y)
                        if (normLen < 8f) onVectorChanged(0f, 0f)
                        else onVectorChanged(knobOffset.x / maxRadiusPx, knobOffset.y / maxRadiusPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(FarmCreamGold.copy(alpha = 0.92f))
                .border(2.dp, FarmWoodDark, CircleShape)
        )
    }
}
