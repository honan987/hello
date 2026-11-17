package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.pixel.PixelPalette
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCharacterPreview
import com.example.ui.components.PixelPanel
import com.example.ui.components.PixelPortraitPreview
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CharacterCreatorScreen(
    initialPlayer: CharacterAppearance,
    initialCrush: CharacterAppearance,
    unlockedOutfits: Set<String>,
    isEditInGame: Boolean,
    onBack: () -> Unit,
    onConfirm: (playerApp: CharacterAppearance, crushApp: CharacterAppearance) -> Unit
) {
    BackHandler { onBack() }

    var editingTarget by remember { mutableIntStateOf(0) } // 0 = Player, 1 = Crush
    var playerApp by remember { mutableStateOf(initialPlayer) }
    var crushApp by remember { mutableStateOf(initialCrush) }

    var previewDir by remember { mutableStateOf(Direction.DOWN) }
    var previewAction by remember { mutableStateOf(CharacterAction.IDLE) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Body/Face, 1 = Hair, 2 = Outfits, 3 = Presets
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200L)
            frame = (frame + 1) % 4
        }
    }

    val currentApp = if (editingTarget == 0) playerApp else crushApp
    fun updateCurrent(transform: (CharacterAppearance) -> CharacterAppearance) {
        if (editingTarget == 0) playerApp = transform(playerApp)
        else crushApp = transform(crushApp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FarmCreamGold, FarmParchment)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = FarmInk)
                }
                Text(
                    text = if (isEditInGame) "Tủ Đồ & Salon" else "Tạo Hình Nhân Vật",
                    style = MaterialTheme.typography.titleLarge,
                    color = FarmWoodDark
                )
            }
            // Player vs Crush Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = editingTarget == 0,
                    onClick = { editingTarget = 0 },
                    label = { Text("Nhân vật") },
                    modifier = Modifier.testTag("creator_tab_player")
                )
                FilterChip(
                    selected = editingTarget == 1,
                    onClick = { editingTarget = 1 },
                    label = { Text("Crush") },
                    modifier = Modifier.testTag("creator_tab_crush")
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Live Real-time Preview Card (4 Directions + Animations + Portrait)
        PixelPanel(
            containerColor = FarmParchmentDeep,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bust Portrait
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelPortraitPreview(
                        appearance = currentApp,
                        blink = frame == 3,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Chân dung", style = MaterialTheme.typography.labelSmall, color = FarmInkMuted)
                }

                // Animated Full Sprite + Rotate Controls
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                previewDir = when (previewDir) {
                                    Direction.DOWN -> Direction.LEFT
                                    Direction.LEFT -> Direction.UP
                                    Direction.UP -> Direction.RIGHT
                                    Direction.RIGHT -> Direction.DOWN
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Xoay trái", tint = FarmWoodDark)
                        }
                        Box(
                            modifier = Modifier
                                .size(92.dp, 108.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2F2D5))
                                .border(2.dp, FarmWoodLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelCharacterPreview(
                                appearance = currentApp,
                                direction = previewDir,
                                action = previewAction,
                                frame = frame,
                                blink = frame == 3,
                                modifier = Modifier.size(72.dp, 96.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                previewDir = when (previewDir) {
                                    Direction.DOWN -> Direction.RIGHT
                                    Direction.RIGHT -> Direction.UP
                                    Direction.UP -> Direction.LEFT
                                    Direction.LEFT -> Direction.DOWN
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Xoay phải", tint = FarmWoodDark)
                        }
                    }
                    // Action Animation Tester Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            CharacterAction.IDLE,
                            CharacterAction.WALK,
                            CharacterAction.RUN,
                            CharacterAction.HOE,
                            CharacterAction.WATER,
                            CharacterAction.WAVE,
                            CharacterAction.LOVE
                        ).forEach { act ->
                            AssistChip(
                                onClick = { previewAction = act },
                                label = { Text(act.labelVi, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (previewAction == act) FarmMint else FarmParchment
                                )
                            )
                        }
                    }
                }

                // Randomize button
                IconButton(
                    onClick = {
                        val g = currentApp.gender
                        val hairs = CharacterCatalog.hairStylesFor(g)
                        val tops = CharacterCatalog.outfitItems.filter { it.category == OutfitCategory.TOP && (isEditInGame.not() || unlockedOutfits.contains(it.id)) }
                        val bots = CharacterCatalog.outfitItems.filter { it.category == OutfitCategory.BOTTOM && (isEditInGame.not() || unlockedOutfits.contains(it.id)) }
                        updateCurrent {
                            it.copy(
                                skinToneIndex = PixelPalette.skinRamps.indices.random(),
                                hairStyleId = hairs.random().id,
                                hairColorIndex = PixelPalette.hairRamps.indices.random(),
                                eyeStyle = EyeStyle.entries.random(),
                                eyeColorIndex = PixelPalette.eyeColors.indices.random(),
                                topId = tops.randomOrNull()?.id ?: it.topId,
                                topColorIndex = PixelPalette.clothRamps.indices.random(),
                                bottomId = bots.randomOrNull()?.id ?: it.bottomId,
                                bottomColorIndex = PixelPalette.clothRamps.indices.random()
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FarmCreamGold)
                        .border(2.dp, FarmWoodDark, CircleShape)
                        .testTag("randomize_character_button")
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Ngẫu nhiên", tint = FarmInk)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Category Tabs
        val tabs = listOf("Khuôn mặt", "Kiểu tóc (12+)", "Trang phục", "Bộ phối sẵn")
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = FarmParchmentDeep,
            edgePadding = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tab Body
        PixelPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        // Gender Selection
                        Text("Giới tính", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gender.entries.forEach { g ->
                                FilterChip(
                                    selected = currentApp.gender == g,
                                    onClick = {
                                        val firstHair = CharacterCatalog.hairStylesFor(g).first().id
                                        updateCurrent { it.copy(gender = g, hairStyleId = firstHair) }
                                    },
                                    label = { Text(g.labelVi) }
                                )
                            }
                        }

                        // Skin Tones
                        Text("Màu da", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PixelPalette.skinRamps.forEachIndexed { idx, ramp ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(ramp.base))
                                        .border(
                                            if (currentApp.skinToneIndex == idx) 3.dp else 1.5.dp,
                                            if (currentApp.skinToneIndex == idx) FarmRose else FarmWoodDark,
                                            CircleShape
                                        )
                                        .clickable { updateCurrent { it.copy(skinToneIndex = idx) } }
                                )
                            }
                        }

                        // Eye Style
                        Text("Kiểu mắt & Lông mày", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EyeStyle.entries.forEach { es ->
                                FilterChip(
                                    selected = currentApp.eyeStyle == es,
                                    onClick = { updateCurrent { it.copy(eyeStyle = es) } },
                                    label = { Text(es.labelVi) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EyebrowStyle.entries.forEach { eb ->
                                FilterChip(
                                    selected = currentApp.eyebrowStyle == eb,
                                    onClick = { updateCurrent { it.copy(eyebrowStyle = eb) } },
                                    label = { Text("Mày ${eb.labelVi}") }
                                )
                            }
                        }

                        // Eye Color
                        Text("Màu mắt", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelPalette.eyeColors.forEachIndexed { idx, (_, col) ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(col))
                                        .border(
                                            if (currentApp.eyeColorIndex == idx) 3.dp else 1.5.dp,
                                            if (currentApp.eyeColorIndex == idx) FarmRose else FarmWoodDark,
                                            CircleShape
                                        )
                                        .clickable { updateCurrent { it.copy(eyeColorIndex = idx) } }
                                )
                            }
                        }

                        // Expressions
                        Text("Biểu cảm khuôn mặt", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Expression.entries.forEach { exp ->
                                FilterChip(
                                    selected = currentApp.expression == exp,
                                    onClick = { updateCurrent { it.copy(expression = exp) } },
                                    label = { Text(exp.labelVi) }
                                )
                            }
                        }
                    }

                    1 -> {
                        Text("Màu tóc (10 màu pastel & tự nhiên)", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelPalette.hairRamps.forEachIndexed { idx, ramp ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(ramp.base))
                                        .border(
                                            if (currentApp.hairColorIndex == idx) 3.dp else 1.5.dp,
                                            if (currentApp.hairColorIndex == idx) FarmRose else FarmWoodDark,
                                            CircleShape
                                        )
                                        .clickable { updateCurrent { it.copy(hairColorIndex = idx) } }
                                )
                            }
                        }

                        val hairList = CharacterCatalog.hairStylesFor(currentApp.gender)
                        Text("Kiểu tóc (${currentApp.gender.labelVi} - ${hairList.size} kiểu)", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        hairList.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { h ->
                                    val selected = currentApp.hairStyleId == h.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) FarmMint else FarmParchmentDeep)
                                            .border(2.dp, if (selected) FarmMintDark else FarmWoodLight, RoundedCornerShape(8.dp))
                                            .clickable { updateCurrent { it.copy(hairStyleId = h.id) } }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(h.nameVi, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    2 -> {
                        // Color tint pickers for Top & Bottom
                        Text("Màu Áo", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelPalette.clothRamps.forEachIndexed { idx, ramp ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(ramp.base))
                                        .border(
                                            if (currentApp.topColorIndex == idx) 3.dp else 1.5.dp,
                                            if (currentApp.topColorIndex == idx) FarmRose else FarmWoodDark,
                                            CircleShape
                                        )
                                        .clickable { updateCurrent { it.copy(topColorIndex = idx) } }
                                )
                            }
                        }

                        Text("Màu Quần / Váy", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelPalette.clothRamps.forEachIndexed { idx, ramp ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(ramp.base))
                                        .border(
                                            if (currentApp.bottomColorIndex == idx) 3.dp else 1.5.dp,
                                            if (currentApp.bottomColorIndex == idx) FarmRose else FarmWoodDark,
                                            CircleShape
                                        )
                                        .clickable { updateCurrent { it.copy(bottomColorIndex = idx) } }
                                )
                            }
                        }

                        OutfitCategory.entries.forEach { cat ->
                            Text(cat.labelVi, style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                            val items = CharacterCatalog.outfitItems.filter {
                                it.category == cat && (!isEditInGame || editingTarget == 1 || unlockedOutfits.contains(it.id))
                            }
                            FlowChipGrid(items = items, current = currentApp) { picked ->
                                updateCurrent {
                                    when (picked.category) {
                                        OutfitCategory.TOP -> it.copy(topId = picked.id)
                                        OutfitCategory.BOTTOM -> it.copy(bottomId = picked.id)
                                        OutfitCategory.SHOES -> it.copy(shoesId = picked.id)
                                        OutfitCategory.HAT -> it.copy(hatId = picked.id)
                                        OutfitCategory.ACCESSORY -> it.copy(accessoryId = picked.id)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        val presets = if (editingTarget == 0) CharacterCatalog.playerPresets else CharacterCatalog.crushPresets
                        Text(
                            text = if (editingTarget == 0) "Bộ trang phục Player (P01–P08)" else "Bộ trang phục Crush (C01–C08)",
                            style = MaterialTheme.typography.titleMedium,
                            color = FarmWoodDark
                        )
                        presets.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FarmParchmentDeep)
                                    .border(1.5.dp, FarmWoodLight, RoundedCornerShape(8.dp))
                                    .clickable {
                                        updateCurrent {
                                            it.copy(
                                                topId = p.topId,
                                                topColorIndex = p.topColor,
                                                bottomId = p.bottomId,
                                                bottomColorIndex = p.bottomColor,
                                                shoesId = p.shoesId,
                                                hatId = p.hatId,
                                                accessoryId = p.accessoryId
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${p.code}: ${p.nameVi}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Mặc ngay", style = MaterialTheme.typography.labelMedium, color = FarmMintDark)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        PixelButton(
            text = if (isEditInGame) "Lưu thay đổi ngoại hình" else "Vào Nông Trại Ngay!",
            onClick = { onConfirm(playerApp, crushApp) },
            icon = Icons.Filled.Check,
            containerColor = FarmLeafGreen,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            testTag = "confirm_character_button"
        )
    }
}

@Composable
private fun FlowChipGrid(
    items: List<OutfitItemDef>,
    current: CharacterAppearance,
    onPick: (OutfitItemDef) -> Unit
) {
    items.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { item ->
                val selected = when (item.category) {
                    OutfitCategory.TOP -> current.topId == item.id
                    OutfitCategory.BOTTOM -> current.bottomId == item.id
                    OutfitCategory.SHOES -> current.shoesId == item.id
                    OutfitCategory.HAT -> current.hatId == item.id
                    OutfitCategory.ACCESSORY -> current.accessoryId == item.id
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) FarmBlushPink.copy(alpha = 0.45f) else FarmParchmentDeep)
                        .border(2.dp, if (selected) FarmRose else FarmWoodLight, RoundedCornerShape(8.dp))
                        .clickable { onPick(item) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(item.nameVi, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
