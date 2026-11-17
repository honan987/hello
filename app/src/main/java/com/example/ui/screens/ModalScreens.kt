package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.game.npc.CrushSystem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
private fun ScreenFrame(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FarmCreamGold, FarmParchment)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                Text(title, style = MaterialTheme.typography.headlineMedium, color = FarmWoodDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun InventoryScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onEquipSlot: (Int) -> Unit,
    onEatItem: (InventoryItem) -> Unit,
    onGiftItem: (InventoryItem) -> Unit,
    onSellItem: (InventoryItem) -> Unit
) {
    ScreenFrame(
        title = "Túi Đồ Nông Trại",
        onBack = onBack,
        trailing = {
            Text("${state.coins} xu", style = MaterialTheme.typography.labelLarge, color = FarmWoodDark)
        }
    ) {
        state.inventory.forEachIndexed { idx, item ->
            PixelPanel(contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FarmParchmentDeep)
                            .border(1.5.dp, FarmWoodDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelItemIcon(item = item, size = 34.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.nameVi}" + if (item.category != ItemCategory.TOOL) " (x${item.quantity})" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = FarmInk
                        )
                        if (item.descriptionVi.isNotBlank()) {
                            Text(item.descriptionVi, style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (idx < 7) {
                        PixelButton(
                            text = if (state.selectedSlotIndex == idx) "Đang cầm" else "Cầm tay",
                            onClick = { onEquipSlot(idx); onBack() },
                            containerColor = FarmLeafGreen
                        )
                    }
                    if (item.energyRestore > 0) {
                        PixelButton(
                            text = "Ăn (+${item.energyRestore} NL)",
                            onClick = { onEatItem(item) },
                            containerColor = FarmSunGold,
                            contentColor = FarmInk
                        )
                    }
                    if (item.category == ItemCategory.CROP || item.category == ItemCategory.GIFT_ITEM) {
                        PixelButton(
                            text = "Bán (+${item.sellPrice})",
                            onClick = { onSellItem(item) },
                            containerColor = FarmCreamGold,
                            contentColor = FarmInk
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onBuySeed: (CropType) -> Unit,
    onBuySnack: () -> Unit,
    onBuyGift: () -> Unit,
    onBuyOrEquipOutfit: (OutfitItemDef) -> Unit,
    onSellAllCrops: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = Seeds & Items, 1 = Tailor & Salon Outfits
    ScreenFrame(
        title = "Cửa Hàng & Thời Trang",
        onBack = onBack,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = FarmSunGold)
                Spacer(Modifier.width(4.dp))
                Text("${state.coins} xu", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Hạt giống & Quà") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Tiệm May Thời Trang") })
        }

        if (tab == 0) {
            PixelButton(
                text = "Bán toàn bộ nông sản trong túi",
                onClick = onSellAllCrops,
                containerColor = FarmSunGold,
                contentColor = FarmInk,
                modifier = Modifier.fillMaxWidth()
            )
            PixelPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bánh mật ong (+40 Năng lượng)", style = MaterialTheme.typography.titleMedium)
                        Text("Giá: 35 xu", style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                    }
                    PixelButton(text = "Mua 35 xu", onClick = onBuySnack)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bó hoa đồng nội (Quà tặng Crush)", style = MaterialTheme.typography.titleMedium)
                        Text("Giá: 75 xu • +12 Thiện cảm", style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                    }
                    PixelButton(text = "Mua 75 xu", onClick = onBuyGift, containerColor = FarmRose)
                }
            }

            CropType.entries.forEach { crop ->
                PixelPanel(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Hạt ${crop.nameVi}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Trồng ${crop.daysToGrow} ngày • Bán ${crop.sellPrice} xu • ${crop.descriptionVi}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FarmInkMuted
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        PixelButton(
                            text = "${crop.seedPrice} xu",
                            onClick = { onBuySeed(crop) },
                            testTag = "buy_seed_${crop.id}"
                        )
                    }
                }
            }
        } else {
            val shopOutfits = CharacterCatalog.outfitItems.filter { it.price > 0 }
            shopOutfits.forEach { outfit ->
                val owned = state.unlockedOutfits.contains(outfit.id)
                PixelPanel(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(outfit.nameVi, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${outfit.category.labelVi} • ${if (owned) "Đã sở hữu" else "${outfit.price} xu"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FarmInkMuted
                            )
                        }
                        PixelButton(
                            text = if (owned) "Mặc ngay" else "Mua ${outfit.price} xu",
                            onClick = { onBuyOrEquipOutfit(outfit) },
                            containerColor = if (owned) FarmMintDark else FarmRose
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterProfileScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onOpenSalonEditor: () -> Unit
) {
    ScreenFrame(title = "Hồ Sơ Nhân Vật", onBack = onBack) {
        PixelPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelPortraitPreview(
                    appearance = state.playerAppearance,
                    modifier = Modifier.size(84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.playerName, style = MaterialTheme.typography.headlineMedium, color = FarmWoodDark)
                    Text("Nông trại: ${state.farmName}", style = MaterialTheme.typography.bodyLarge)
                    Text("Giới tính: ${state.playerAppearance.gender.labelVi}", style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                    Text("Ngày thứ ${state.day} • ${state.coins} xu", style = MaterialTheme.typography.labelMedium, color = FarmMintDark)
                }
            }
            Spacer(Modifier.height(14.dp))
            PixelButton(
                text = "Vào Salon & Đổi Trang Phục",
                onClick = onOpenSalonEditor,
                icon = Icons.Filled.Checkroom,
                containerColor = FarmRose,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open_salon_button"
            )
        }

        PixelPanel {
            Text("Thống kê khu vườn", style = MaterialTheme.typography.titleLarge, color = FarmWoodDark)
            Spacer(Modifier.height(6.dp))
            val plantedCount = state.plots.values.count { it.cropType != null }
            val matureCount = state.plots.values.count { it.isMature }
            Text("• Ô đất đang trồng cây: $plantedCount")
            Text("• Cây sẵn sàng thu hoạch: $matureCount")
            Text("• Trang phục đã mở khóa: ${state.unlockedOutfits.size} món")
            Text("• Kỷ niệm đã lưu giữ: ${state.memories.size}")
        }
    }
}

@Composable
fun RelationshipScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onStartDate: (String) -> Unit,
    onGiveGift: (InventoryItem) -> Unit
) {
    ScreenFrame(title = "Tình Cảm & Lịch Trình", onBack = onBack) {
        val hour = (state.minuteOfDay / 60) % 24
        val schedule = CrushSystem.currentScheduleFor(hour)

        PixelPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelPortraitPreview(
                    appearance = state.crushAppearance,
                    expression = if (state.affection >= 60) Expression.HAPPY else Expression.NEUTRAL,
                    modifier = Modifier.size(84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.crushDisplayName, style = MaterialTheme.typography.headlineMedium, color = FarmRose)
                    Text("Cấp độ: ${state.relationshipTier.labelVi}", style = MaterialTheme.typography.titleMedium, color = FarmWoodDark)
                    Spacer(Modifier.height(4.dp))
                    AffectionHeartsRow(affection = state.affection)
                    Spacer(Modifier.height(4.dp))
                    Text("Hiện tại: ${schedule.activityTextVi}", style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                }
            }
        }

        // Playable Dates
        PixelPanel {
            Text("Những Buổi Hẹn Hò", style = MaterialTheme.typography.titleLarge, color = FarmWoodDark)
            Spacer(Modifier.height(8.dp))
            val dates = listOf(
                Triple("date_meadow_picnic", "Dã ngoại Đồng Hoa", 25),
                Triple("date_sunset_lake", "Ngắm hoàng hôn bên Hồ Nước", 50),
                Triple("date_secret_glade", "Đêm đom đóm Khu Vực Bí Mật", 80)
            )
            dates.forEach { (id, label, req) ->
                val unlocked = state.affection >= req
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (unlocked) "Đã mở khóa!" else "Cần $req/100 Thiện cảm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (unlocked) FarmMintDark else FarmInkMuted
                        )
                    }
                    PixelButton(
                        text = if (state.completedDates.contains(id)) "Hẹn lại" else "Mời hẹn",
                        onClick = { onStartDate(id); onBack() },
                        enabled = unlocked,
                        containerColor = FarmRose
                    )
                }
            }
        }

        // Quick Gift Picker
        val giftables = state.inventory.filter { it.category == ItemCategory.CROP || it.category == ItemCategory.GIFT_ITEM }
        PixelPanel {
            Text("Tặng quà nhanh từ túi đồ", style = MaterialTheme.typography.titleLarge, color = FarmWoodDark)
            Spacer(Modifier.height(6.dp))
            if (giftables.isEmpty()) {
                Text("Hãy thu hoạch Dâu tây, Hoa hoặc mua Bó hoa ở Cửa hàng để tặng nhé!", style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
            } else {
                giftables.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${item.nameVi} (x${item.quantity})", style = MaterialTheme.typography.bodyLarge)
                        PixelButton(
                            text = "Tặng",
                            onClick = { onGiveGift(item); onBack() },
                            containerColor = FarmRose
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen(
    state: GameUiState,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    ScreenFrame(title = "Nhật Ký & Nhiệm Vụ", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Nhiệm vụ (${state.quests.count { it.completed }}/${state.quests.size})") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Nhật ký tình yêu & Kỷ niệm") })
        }

        if (tab == 0) {
            state.quests.forEach { q ->
                PixelPanel(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(q.titleVi, style = MaterialTheme.typography.titleMedium, color = if (q.completed) FarmMintDark else FarmInk)
                            Text(q.descriptionVi, style = MaterialTheme.typography.bodyMedium, color = FarmInkMuted)
                            Text("Thưởng: +${q.rewardCoins} xu, +${q.rewardAffection} thiện cảm", style = MaterialTheme.typography.labelSmall, color = FarmRose)
                        }
                        Text(
                            text = if (q.completed) "Xong ✓" else "${q.progress}/${q.target}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (q.completed) FarmMintDark else FarmWoodDark
                        )
                    }
                }
            }
        } else {
            if (state.memories.isEmpty()) {
                PixelPanel {
                    Text("Chưa có kỷ niệm nào. Hãy trò chuyện hoặc hẹn hò cùng Crush nhé!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                state.memories.forEach { m ->
                    PixelPanel(contentPadding = PaddingValues(12.dp)) {
                        Text(
                            text = if (m.isLoveDiary) "♥ ${m.titleVi}" else "✿ ${m.titleVi}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (m.isLoveDiary) FarmRose else FarmWoodDark
                        )
                        Text("Ngày ${m.day} • ${m.zoneNameVi}", style = MaterialTheme.typography.labelSmall, color = FarmInkMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(m.descriptionVi, style = MaterialTheme.typography.bodyMedium, color = FarmInk)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: GameUiState,
    inGame: Boolean,
    onBack: () -> Unit,
    onUpdateAudio: (bgm: Float?, sfx: Float?, muted: Boolean?) -> Unit,
    onSaveNow: () -> Unit,
    onReturnMainMenu: () -> Unit
) {
    ScreenFrame(title = "Cài Đặt Âm Thanh & Hệ Thống", onBack = onBack) {
        PixelPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tắt toàn bộ âm thanh", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.audioSettings.isMuted,
                    onCheckedChange = { onUpdateAudio(null, null, it) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Nhạc nền Piano (${(state.audioSettings.bgmVolume * 100).toInt()}%)", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = state.audioSettings.bgmVolume,
                onValueChange = { onUpdateAudio(it, null, null) }
            )
            Spacer(Modifier.height(8.dp))
            Text("Hiệu ứng âm thanh (${(state.audioSettings.sfxVolume * 100).toInt()}%)", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = state.audioSettings.sfxVolume,
                onValueChange = { onUpdateAudio(null, it, null) }
            )
        }

        if (inGame) {
            PixelPanel {
                PixelButton(
                    text = "Lưu tiến trình ngay",
                    onClick = onSaveNow,
                    containerColor = FarmLeafGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                PixelButton(
                    text = "Lưu & Quay về Menu Chính",
                    onClick = { onSaveNow(); onReturnMainMenu() },
                    containerColor = FarmWoodLight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
