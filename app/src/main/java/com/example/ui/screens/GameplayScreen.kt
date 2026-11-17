package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.game.model.*
import com.example.game.npc.CrushSystem
import com.example.game.pixel.CharacterSpriteRenderer
import com.example.game.pixel.WorldTileAtlas
import com.example.game.world.FarmWorldMap
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState
import com.example.viewmodel.ParticleKind
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DrawableEntity(
    val sortY: Float,
    val draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
)

@Composable
fun GameplayScreen(
    state: GameUiState,
    onJoystickMove: (Float, Float) -> Unit,
    onTapWorldTile: (Int, Int) -> Unit,
    onPrimaryAction: () -> Unit,
    onToggleRun: () -> Unit,
    onSelectHotbar: (Int) -> Unit,
    onSelectDialogueOption: (DialogueOption) -> Unit,
    onCloseDialogue: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenRelationship: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenSettings: () -> Unit,
    onSleepDay: () -> Unit
) {
    var showWorldMap by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF223322))) {
        // FULLSCREEN 2D PIXEL ART WORLD CANVAS (Multi-Layer + Y-Sorting + Lighting + Weather)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.playerX, state.playerY) {
                    detectTapGestures { offset ->
                        val tilePx = (minOf(size.width, size.height) / 11f).coerceIn(48f, 96f)
                        val camX = state.playerX * tilePx - size.width / 2f
                        val camY = state.playerY * tilePx - size.height / 2f
                        val wx = ((offset.x + camX) / tilePx).toInt()
                        val wy = ((offset.y + camY) / tilePx).toInt()
                        onTapWorldTile(wx, wy)
                    }
                }
        ) {
            val tilePx = (minOf(size.width, size.height) / 11f).coerceIn(48f, 96f)
            val camX = state.playerX * tilePx - size.width / 2f
            val camY = state.playerY * tilePx - size.height / 2f

            val minTx = (camX / tilePx).toInt().coerceAtLeast(0) - 1
            val maxTx = ((camX + size.width) / tilePx).toInt().coerceAtMost(FarmWorldMap.WIDTH - 1) + 1
            val minTy = (camY / tilePx).toInt().coerceAtLeast(0) - 1
            val maxTy = ((camY + size.height) / tilePx).toInt().coerceAtMost(FarmWorldMap.HEIGHT - 1) + 1

            val isSunset = state.currentTimePhase == TimePhase.SUNSET
            val isNight = state.currentTimePhase == TimePhase.EVENING || state.currentTimePhase == TimePhase.NIGHT

            // LAYER 1 & 2: GROUND TILES & FARM SOIL
            for (ty in minTy.coerceAtLeast(0)..maxTy.coerceAtMost(FarmWorldMap.HEIGHT - 1)) {
                for (tx in minTx.coerceAtLeast(0)..maxTx.coerceAtMost(FarmWorldMap.WIDTH - 1)) {
                    val plot = state.plots[tx to ty]
                    val tileType = when {
                        plot != null && plot.watered -> TileType.WATERED_SOIL
                        plot != null && plot.tilled -> TileType.TILLED_SOIL
                        else -> FarmWorldMap.tiles[ty][tx]
                    }
                    val tileBmp = WorldTileAtlas.getTile(tileType, state.animFrame, tx + ty)
                    val sx = (tx * tilePx - camX).roundToInt()
                    val sy = (ty * tilePx - camY).roundToInt()
                    val sz = tilePx.roundToInt() + 1
                    drawImage(
                        image = tileBmp,
                        dstOffset = IntOffset(sx, sy),
                        dstSize = IntSize(sz, sz),
                        filterQuality = FilterQuality.None
                    )
                    // Dramatic Orange-Pink Sunset Reflection on Crystal Lake!
                    if (tileType == TileType.WATER && isSunset) {
                        drawRect(
                            color = Color(0x66FF8E72),
                            topLeft = Offset(sx.toFloat(), sy.toFloat()),
                            size = Size(sz.toFloat(), sz.toFloat())
                        )
                    }
                }
            }

            // LAYER 3: DECORATIONS (Non-solid Flowers) & PLANTED CROPS
            for ((pos, plot) in state.plots) {
                val (tx, ty) = pos
                if (tx !in minTx..maxTx || ty !in minTy..maxTy) continue
                val crop = plot.cropType ?: continue
                val cropBmp = WorldTileAtlas.getCropSprite(crop, plot.growthStage)
                val sx = (tx * tilePx - camX).roundToInt()
                val sy = (ty * tilePx - camY - tilePx * 0.25f).roundToInt()
                drawImage(
                    image = cropBmp,
                    dstOffset = IntOffset(sx, sy),
                    dstSize = IntSize(tilePx.roundToInt(), (tilePx * 1.25f).roundToInt()),
                    filterQuality = FilterQuality.None
                )
            }

            // Highlight targeted farm tile in front of player
            val ftx = when (state.playerDir) {
                Direction.LEFT -> (state.playerX - 0.9f).toInt()
                Direction.RIGHT -> (state.playerX + 0.9f).toInt()
                else -> state.playerX.toInt()
            }
            val fty = when (state.playerDir) {
                Direction.UP -> (state.playerY - 0.9f).toInt()
                Direction.DOWN -> (state.playerY + 0.9f).toInt()
                else -> state.playerY.toInt()
            }
            if (state.plots.containsKey(ftx to fty)) {
                drawRect(
                    color = Color(0xAAFFF59D),
                    topLeft = Offset(ftx * tilePx - camX, fty * tilePx - camY),
                    size = Size(tilePx, tilePx),
                    style = Stroke(width = 3f)
                )
            }

            // LAYER 4 & 5: Y-SORTED PROPS, BUILDINGS, TREES, CRUSH, & PLAYER
            val drawables = mutableListOf<DrawableEntity>()
            val windOffset = sin(state.cloudOffset * 1.8f) * 2.5f

            for (prop in FarmWorldMap.props) {
                if (prop.tileX + prop.type.widthTiles < minTx || prop.tileX > maxTx) continue
                if (prop.tileY + prop.type.heightTiles < minTy || prop.tileY > maxTy) continue
                val bmp = WorldTileAtlas.getPropSprite(prop.type, state.animFrame)
                val isTree = prop.type in setOf(PropType.TREE_OAK, PropType.TREE_PINE, PropType.TREE_SAKURA, PropType.TREE_SMALL)
                val isFlower = prop.type.name.startsWith("FLOWER_")
                val sortY = if (isFlower) -100f else (prop.tileY + prop.type.heightTiles).toFloat()

                drawables += DrawableEntity(sortY) {
                    val wPx = (prop.type.widthTiles * tilePx).roundToInt()
                    val hPx = when (prop.type) {
                        PropType.PLAYER_HOUSE, PropType.CRUSH_HOUSE -> (tilePx * 3.25f).roundToInt()
                        PropType.TREE_OAK, PropType.TREE_PINE, PropType.TREE_SAKURA -> (tilePx * 2.6f).roundToInt()
                        PropType.TREE_SMALL, PropType.LANTERN, PropType.SCARECROW, PropType.WISHING_CRYSTAL, PropType.MIRROR_POST -> (tilePx * 1.4f).roundToInt()
                        else -> (prop.type.heightTiles * tilePx).roundToInt()
                    }
                    val sway = if (isTree || isFlower) windOffset.roundToInt() else 0
                    val px = (prop.tileX * tilePx - camX).roundToInt() + sway
                    val py = ((prop.tileY + prop.type.heightTiles) * tilePx - camY - hPx).roundToInt()

                    // Warm lantern glow pool at evening/night
                    if (prop.type == PropType.LANTERN && (isNight || isSunset)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0x88FFE082), Color.Transparent),
                                center = Offset(px + wPx / 2f, py + hPx * 0.4f),
                                radius = tilePx * 1.8f
                            ),
                            center = Offset(px + wPx / 2f, py + hPx * 0.4f),
                            radius = tilePx * 1.8f
                        )
                    }
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(px, py),
                        dstSize = IntSize(wPx, hPx),
                        filterQuality = FilterQuality.None
                    )
                }
            }

            // Crush Entity
            drawables += DrawableEntity(state.crushY) {
                val sprite = CharacterSpriteRenderer.getSprite(
                    state.crushAppearance, state.crushDir, state.crushAction, state.animFrame, state.blinkOn
                )
                val sw = (tilePx * 1.15f).roundToInt()
                val sh = (tilePx * 1.53f).roundToInt()
                val sx = (state.crushX * tilePx - camX - sw / 2f).roundToInt()
                val sy = (state.crushY * tilePx - camY - sh + tilePx * 0.25f).roundToInt()
                drawImage(sprite, dstOffset = IntOffset(sx, sy), dstSize = IntSize(sw, sh), filterQuality = FilterQuality.None)
            }

            // Player Entity
            drawables += DrawableEntity(state.playerY) {
                val sprite = CharacterSpriteRenderer.getSprite(
                    state.playerAppearance, state.playerDir, state.playerAction, state.animFrame, state.blinkOn
                )
                val sw = (tilePx * 1.15f).roundToInt()
                val sh = (tilePx * 1.53f).roundToInt()
                val sx = (state.playerX * tilePx - camX - sw / 2f).roundToInt()
                val sy = (state.playerY * tilePx - camY - sh + tilePx * 0.25f).roundToInt()
                drawImage(sprite, dstOffset = IntOffset(sx, sy), dstSize = IntSize(sw, sh), filterQuality = FilterQuality.None)
            }

            drawables.sortedBy { it.sortY }.forEach { it.draw(this) }

            // LAYER 6: FOREGROUND PARTICLES, DRIFTING CLOUDS, SUNBEAMS & LIGHTING OVERLAYS
            for (p in state.particles) {
                val px = p.x * tilePx - camX
                val py = p.y * tilePx - camY
                val alpha = (1f - p.life / p.maxLife).coerceIn(0f, 1f)
                when (p.kind) {
                    ParticleKind.LEAF -> drawCircle(Color(0xFFF4A6B8).copy(alpha = alpha), 4f, Offset(px, py))
                    ParticleKind.FIREFLY -> {
                        drawCircle(Color(0xFFFFF176).copy(alpha = alpha * 0.85f), 7f, Offset(px, py))
                        drawCircle(Color.White.copy(alpha = alpha), 2.5f, Offset(px, py))
                    }
                    ParticleKind.RAIN -> drawLine(
                        Color(0xAA90CAF9).copy(alpha = alpha),
                        Offset(px, py),
                        Offset(px - 6f, py + 18f),
                        strokeWidth = 2f
                    )
                    ParticleKind.METEOR -> drawLine(
                        Color.White.copy(alpha = alpha),
                        Offset(px, py),
                        Offset(px - 36f, py - 20f),
                        strokeWidth = 3f
                    )
                    ParticleKind.WATER_DROP -> drawCircle(Color(0xFF4FC3F7).copy(alpha = alpha), 4f, Offset(px, py))
                    ParticleKind.HARVEST_ORB -> drawCircle(Color(0xFFFFD54F).copy(alpha = alpha), 7f, Offset(px, py))
                    ParticleKind.HEART -> drawCircle(Color(0xFFFF6E91).copy(alpha = alpha), 6f, Offset(px, py))
                    ParticleKind.DUST -> drawCircle(Color(0xFFD7CCC8).copy(alpha = alpha), 4f, Offset(px, py))
                }
            }

            // Drifting cloud shadows
            if (state.weather == WeatherType.SUNNY || state.weather == WeatherType.CLOUDY || state.weather == WeatherType.RAINBOW) {
                val cloudX = ((state.cloudOffset * 40f) % (size.width + 300f)) - 150f
                drawOval(
                    color = Color(0x16000000),
                    topLeft = Offset(cloudX, size.height * 0.18f),
                    size = Size(240f, 90f)
                )
            }

            // Sunbeams through trees during morning/afternoon
            if (state.weather == WeatherType.SUNNY && !isNight && !isSunset) {
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0x28FFF8C4), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.65f, size.height * 0.7f)
                    )
                )
            }

            // Rainbow Arc after rain!
            if (state.weather == WeatherType.RAINBOW) {
                val rainbowColors = listOf(
                    Color(0x44FF8A80), Color(0x44FFD180), Color(0x44FFFF8D),
                    Color(0x44B9F6CA), Color(0x4480D8FF), Color(0x44B388FF)
                )
                rainbowColors.forEachIndexed { idx, col ->
                    drawArc(
                        color = col,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.1f + idx * 8f, size.height * 0.08f + idx * 8f),
                        size = Size(size.width * 0.8f - idx * 16f, size.height * 0.45f),
                        style = Stroke(width = 8f)
                    )
                }
            }

            // Atmospheric Lighting Tint by TimePhase & Weather
            val ambientTint = when {
                state.weather == WeatherType.FOGGY -> Color(0x3CDDE7EA)
                state.weather == WeatherType.RAINY -> Color(0x363A5570)
                state.currentTimePhase == TimePhase.DAWN -> Color(0x2EFFC1A6)
                state.currentTimePhase == TimePhase.AFTERNOON -> Color(0x16FFE0B2)
                state.currentTimePhase == TimePhase.SUNSET -> Color(0x48FF8A65)
                state.currentTimePhase == TimePhase.EVENING -> Color(0x523E2B5E)
                state.currentTimePhase == TimePhase.NIGHT -> Color(0x78181D3A)
                else -> Color.Transparent
            }
            if (ambientTint != Color.Transparent) {
                drawRect(color = ambientTint)
            }
        }

        // CRUSH PROXIMITY SPEECH BUBBLE
        if (state.crushBubble != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FarmParchment.copy(alpha = 0.94f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 74.dp)
                    .border(2.dp, FarmRose, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "${state.crushDisplayName}: \"${state.crushBubble}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = FarmInk,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // TOP HUD BAR (Time, Weather, Zone, Coins, Energy)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Day, Clock, Weather, Zone
            PixelPanel(
                containerColor = FarmParchment.copy(alpha = 0.92f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ngày ${state.day} • ${state.formattedClock} (${state.currentTimePhase.labelVi})",
                    style = MaterialTheme.typography.labelMedium,
                    color = FarmWoodDark
                )
                Text(
                    text = "${state.weather.labelVi} • ${state.currentZone.nameVi}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FarmInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Coins & Energy Bar
            PixelPanel(
                containerColor = FarmParchment.copy(alpha = 0.92f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = FarmSunGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${state.coins} xu", style = MaterialTheme.typography.labelMedium, color = FarmWoodDark)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = FarmLeafGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(74.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(FarmParchmentDeep)
                            .border(1.dp, FarmWoodDark, RoundedCornerShape(4.dp))
                    ) {
                        val frac = (state.energy.toFloat() / state.maxEnergy.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .background(if (frac > 0.3f) FarmLeafGreen else FarmRose)
                        )
                    }
                }
            }
        }

        // SIDE SHORTCUT MENU PILL BAR (Inventory, Shop, Profile/Salon, Crush, Journal, Map, Sleep, Settings)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val shortcuts = listOf(
                Triple(Icons.Filled.Backpack, "Túi đồ", onOpenInventory),
                Triple(Icons.Filled.Storefront, "Cửa hàng", onOpenShop),
                Triple(Icons.Filled.Favorite, "Tình cảm", onOpenRelationship),
                Triple(Icons.AutoMirrored.Filled.MenuBook, "Nhật ký", onOpenJournal),
                Triple(Icons.Filled.Checkroom, "Hồ sơ", onOpenProfile),
                Triple(Icons.Filled.Map, "Bản đồ") { showWorldMap = true },
                Triple(Icons.Filled.Bedtime, "Ngủ qua ngày", onSleepDay),
                Triple(Icons.Filled.Settings, "Cài đặt", onOpenSettings)
            )
            shortcuts.forEach { (icon, desc, action) ->
                Surface(
                    onClick = action,
                    shape = CircleShape,
                    color = FarmParchment.copy(alpha = 0.92f),
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, FarmWoodDark, CircleShape)
                        .testTag("hud_${desc}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = desc, tint = FarmWoodDark, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // TOAST FEEDBACK BANNER
        AnimatedVisibility(
            visible = state.toastMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 112.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FarmWoodDark.copy(alpha = 0.9f),
                modifier = Modifier.border(1.5.dp, FarmCreamGold, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = state.toastMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FarmParchment,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // BOTTOM CONTROLS: JOYSTICK + HOTBAR + ACTION / RUN BUTTONS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Left: Virtual Joystick
            VirtualJoystick(
                onVectorChanged = onJoystickMove,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Center: Hotbar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 132.dp)
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(10.dp))
                    .background(FarmWoodDark.copy(alpha = 0.85f))
                    .border(2.dp, FarmWoodLight, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.inventory.take(7).forEachIndexed { idx, item ->
                    val selected = state.selectedSlotIndex == idx
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) FarmCreamGold else FarmParchment.copy(alpha = 0.85f))
                            .border(
                                if (selected) 2.5.dp else 1.dp,
                                if (selected) FarmRose else FarmWoodDark,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectHotbar(idx) }
                            .testTag("hotbar_slot_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelItemIcon(item = item, size = 30.dp)
                        if (item.category != ItemCategory.TOOL) {
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FarmWoodDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            // Right: Run Toggle + Primary Action Button
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleRun,
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.isRunning) FarmSunGold else FarmParchment.copy(alpha = 0.9f),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
                        .border(2.dp, FarmWoodDark, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DirectionsRun, contentDescription = "Chạy", tint = FarmInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.isRunning) "Chạy" else "Đi", style = MaterialTheme.typography.labelMedium, color = FarmInk)
                    }
                }

                PixelButton(
                    text = "Tác động",
                    onClick = onPrimaryAction,
                    containerColor = FarmLeafGreen,
                    testTag = "primary_action_button"
                )
            }
        }

        // DIALOGUE & DATE EVENT OVERLAY
        val dlg = state.activeDialogue
        if (dlg != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x771E120C))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                PixelPanel(
                    containerColor = FarmParchment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelPortraitPreview(
                                appearance = state.crushAppearance,
                                expression = dlg.expression,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(dlg.speakerName, style = MaterialTheme.typography.labelLarge, color = FarmRose)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (dlg.isDateEvent) {
                                Text("★ Sự kiện hẹn hò lãng mạn", style = MaterialTheme.typography.labelMedium, color = FarmRose)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(dlg.textVi, style = MaterialTheme.typography.bodyLarge, color = FarmInk)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (dlg.options.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dlg.options.forEachIndexed { idx, opt ->
                                PixelButton(
                                    text = opt.labelVi,
                                    onClick = { onSelectDialogueOption(opt) },
                                    containerColor = FarmCreamGold,
                                    contentColor = FarmInk,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "dialogue_option_$idx"
                                )
                            }
                        }
                    } else {
                        PixelButton(
                            text = "Tiếp tục",
                            onClick = onCloseDialogue,
                            containerColor = FarmLeafGreen,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "dialogue_close_button"
                        )
                    }
                }
            }
        }

        // WORLD MINI-MAP MODAL
        if (showWorldMap) {
            Dialog(onDismissRequest = { showWorldMap = false }) {
                PixelPanel {
                    Text("Bản Đồ ${state.farmName}", style = MaterialTheme.typography.titleLarge, color = FarmWoodDark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Vị trí hiện tại: ${state.currentZone.nameVi} • ${state.crushDisplayName}: ${
                            CrushSystem.currentScheduleFor((state.minuteOfDay / 60) % 24).activityTextVi
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FarmInkMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    val zones = WorldZone.entries
                    Column(
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        zones.forEach { z ->
                            val here = state.currentZone == z
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (here) FarmMint else FarmParchmentDeep)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(z.nameVi, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (here) Text("Đang ở đây", style = MaterialTheme.typography.labelSmall, color = FarmMintDark)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PixelButton(
                        text = "Đóng bản đồ",
                        onClick = { showWorldMap = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
