package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.Gender
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCharacterPreview
import com.example.ui.components.PixelPanel
import com.example.ui.theme.*
import com.example.game.model.CharacterAction
import com.example.game.model.CharacterCatalog
import com.example.game.model.Direction
import kotlinx.coroutines.delay

@Composable
fun MainMenuScreen(
    hasSavedGame: Boolean,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onSettings: () -> Unit
) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(220L)
            frame = (frame + 1) % 4
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_menu_hero),
            contentDescription = "Cảnh nông trại hoàng hôn",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x553B2820),
                            Color(0x223B2820),
                            Color(0xAA2D1C14)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Title Banner
            PixelPanel(
                containerColor = FarmParchment.copy(alpha = 0.94f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OUR LITTLE FARM 2.0",
                        style = MaterialTheme.typography.displaySmall,
                        color = FarmWoodDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Khu Vườn Có Cậu • Pixel Art Cottagecore",
                        style = MaterialTheme.typography.titleMedium,
                        color = FarmRose,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Animated Pixel Couple Preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                PixelCharacterPreview(
                    appearance = CharacterCatalog.defaultPlayerAppearance(Gender.FEMALE),
                    direction = Direction.DOWN,
                    action = CharacterAction.WAVE,
                    frame = frame,
                    blink = frame == 3,
                    modifier = Modifier.size(84.dp, 112.dp)
                )
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = FarmBlushPink,
                    modifier = Modifier
                        .padding(bottom = 52.dp)
                        .size(26.dp)
                )
                PixelCharacterPreview(
                    appearance = CharacterCatalog.defaultCrushAppearance(Gender.MALE),
                    direction = Direction.DOWN,
                    action = CharacterAction.HAPPY,
                    frame = frame,
                    blink = frame == 1,
                    modifier = Modifier.size(84.dp, 112.dp)
                )
            }

            // Action Buttons Card
            PixelPanel(
                containerColor = FarmParchment.copy(alpha = 0.96f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                if (hasSavedGame) {
                    PixelButton(
                        text = "Tiếp tục nông trại",
                        onClick = onContinue,
                        icon = Icons.Filled.PlayArrow,
                        containerColor = FarmLeafGreen,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_game_button"
                    )
                    Spacer(Modifier.height(10.dp))
                }
                PixelButton(
                    text = "Bắt đầu hành trình mới",
                    onClick = onNewGame,
                    icon = Icons.Filled.Spa,
                    containerColor = if (hasSavedGame) FarmWoodLight else FarmLeafGreen,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "new_game_button"
                )
                Spacer(Modifier.height(10.dp))
                PixelButton(
                    text = "Cài đặt âm thanh",
                    onClick = onSettings,
                    icon = Icons.Filled.Settings,
                    containerColor = FarmCreamGold,
                    contentColor = FarmInk,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "menu_settings_button"
                )
            }
        }
    }
}

@Composable
fun NewGameScreen(
    onBack: () -> Unit,
    onProceedToCreator: (playerName: String, farmName: String, crushGender: Gender, crushName: String) -> Unit
) {
    var playerName by remember { mutableStateOf("An") }
    var farmName by remember { mutableStateOf("Khu Vườn Có Cậu") }
    var crushGender by remember { mutableStateOf(Gender.MALE) }
    var crushName by remember { mutableStateOf("Minh") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(FarmCreamGold, FarmParchment))
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("new_game_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = FarmInk)
            }
            Text(
                text = "Khởi Tạo Nông Trại Mới",
                style = MaterialTheme.typography.headlineMedium,
                color = FarmWoodDark
            )
        }

        Spacer(Modifier.height(14.dp))

        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Text("Tên nhân vật của bạn", style = MaterialTheme.typography.titleMedium, color = FarmInk)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = { if (it.length <= 20) playerName = it },
                singleLine = true,
                placeholder = { Text("Ví dụ: An, Bảo, Vy...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_name_input")
            )

            Spacer(Modifier.height(14.dp))
            Text("Tên khu vườn nhỏ", style = MaterialTheme.typography.titleMedium, color = FarmInk)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = farmName,
                onValueChange = { if (it.length <= 28) farmName = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farm_name_input")
            )

            Spacer(Modifier.height(16.dp))
            Text("Chọn người thương (Crush)", style = MaterialTheme.typography.titleMedium, color = FarmInk)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Gender.entries.forEach { g ->
                    val selected = crushGender == g
                    val defaultLabel = if (g == Gender.MALE) "Minh (Nam)" else "Linh (Nữ)"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) FarmBlushPink.copy(alpha = 0.55f) else FarmParchmentDeep)
                            .border(
                                2.dp,
                                if (selected) FarmRose else FarmWoodLight,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                crushGender = g
                                crushName = if (g == Gender.MALE) "Minh" else "Linh"
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = defaultLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = FarmInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Tên gọi của Crush", style = MaterialTheme.typography.titleMedium, color = FarmInk)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = crushName,
                onValueChange = { if (it.length <= 20) crushName = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crush_name_input")
            )

            Spacer(Modifier.height(20.dp))
            PixelButton(
                text = "Tạo hình nhân vật →",
                onClick = {
                    onProceedToCreator(
                        playerName.ifBlank { "An" },
                        farmName.ifBlank { "Khu Vườn Có Cậu" },
                        crushGender,
                        crushName.ifBlank { if (crushGender == Gender.MALE) "Minh" else "Linh" }
                    )
                },
                containerColor = FarmLeafGreen,
                modifier = Modifier.fillMaxWidth(),
                testTag = "proceed_creator_button"
            )
        }
    }
}
