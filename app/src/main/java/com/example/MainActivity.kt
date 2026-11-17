package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.model.CharacterCatalog
import com.example.game.model.Gender
import com.example.ui.screens.*
import com.example.ui.theme.OurLittleFarmTheme
import com.example.viewmodel.GameViewModel

object AppRoutes {
    const val MAIN_MENU = "main_menu"
    const val NEW_GAME = "new_game"
    const val CHARACTER_CREATOR = "character_creator"
    const val GAMEPLAY = "gameplay"
    const val INVENTORY = "inventory"
    const val SHOP = "shop"
    const val PROFILE = "profile"
    const val SALON = "salon"
    const val RELATIONSHIP = "relationship"
    const val JOURNAL = "journal"
    const val SETTINGS_MENU = "settings_menu"
    const val SETTINGS_GAME = "settings_game"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OurLittleFarmTheme {
                val vm: GameViewModel = viewModel()
                val state by vm.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                // Pending New Game parameters passed from NewGameScreen to CharacterCreatorScreen
                var pendingPlayerName by remember { mutableStateOf("An") }
                var pendingFarmName by remember { mutableStateOf("Khu Vườn Có Cậu") }
                var pendingCrushGender by remember { mutableStateOf(Gender.MALE) }
                var pendingCrushName by remember { mutableStateOf("Minh") }

                LaunchedEffect(Unit) {
                    vm.startMusicIfNeeded()
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.MAIN_MENU
                ) {
                    composable(AppRoutes.MAIN_MENU) {
                        MainMenuScreen(
                            hasSavedGame = state.hasExistingSave,
                            onContinue = {
                                vm.continueSavedGame { ok ->
                                    if (ok) {
                                        navController.navigate(AppRoutes.GAMEPLAY) {
                                            popUpTo(AppRoutes.MAIN_MENU) { inclusive = true }
                                        }
                                    }
                                }
                            },
                            onNewGame = { navController.navigate(AppRoutes.NEW_GAME) },
                            onSettings = { navController.navigate(AppRoutes.SETTINGS_MENU) }
                        )
                    }

                    composable(AppRoutes.NEW_GAME) {
                        NewGameScreen(
                            onBack = { navController.popBackStack() },
                            onProceedToCreator = { pName, fName, cGender, cName ->
                                pendingPlayerName = pName
                                pendingFarmName = fName
                                pendingCrushGender = cGender
                                pendingCrushName = cName
                                navController.navigate(AppRoutes.CHARACTER_CREATOR)
                            }
                        )
                    }

                    composable(AppRoutes.CHARACTER_CREATOR) {
                        val initPlayer = remember { CharacterCatalog.defaultPlayerAppearance(Gender.FEMALE) }
                        val initCrush = remember(pendingCrushGender) {
                            CharacterCatalog.defaultCrushAppearance(pendingCrushGender)
                        }
                        CharacterCreatorScreen(
                            initialPlayer = initPlayer,
                            initialCrush = initCrush,
                            unlockedOutfits = state.unlockedOutfits,
                            isEditInGame = false,
                            onBack = { navController.popBackStack() },
                            onConfirm = { playerApp, crushApp ->
                                vm.initializeNewGame(
                                    playerName = pendingPlayerName,
                                    farmName = pendingFarmName,
                                    crushGender = crushApp.gender,
                                    crushName = pendingCrushName,
                                    playerAppearance = playerApp,
                                    crushAppearance = crushApp
                                )
                                navController.navigate(AppRoutes.GAMEPLAY) {
                                    popUpTo(AppRoutes.MAIN_MENU) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(AppRoutes.GAMEPLAY) {
                        LaunchedEffect(Unit) { vm.setPaused(false) }
                        GameplayScreen(
                            state = state,
                            onJoystickMove = vm::setJoystickVector,
                            onTapWorldTile = vm::onWorldTileTapped,
                            onPrimaryAction = vm::performPrimaryAction,
                            onToggleRun = vm::toggleRunMode,
                            onSelectHotbar = vm::selectHotbarSlot,
                            onSelectDialogueOption = vm::selectDialogueOption,
                            onCloseDialogue = vm::closeDialogue,
                            onOpenInventory = { vm.setPaused(true); navController.navigate(AppRoutes.INVENTORY) },
                            onOpenShop = { vm.setPaused(true); navController.navigate(AppRoutes.SHOP) },
                            onOpenProfile = { vm.setPaused(true); navController.navigate(AppRoutes.PROFILE) },
                            onOpenRelationship = { vm.setPaused(true); navController.navigate(AppRoutes.RELATIONSHIP) },
                            onOpenJournal = { vm.setPaused(true); navController.navigate(AppRoutes.JOURNAL) },
                            onOpenSettings = { vm.setPaused(true); navController.navigate(AppRoutes.SETTINGS_GAME) },
                            onSleepDay = vm::sleepAndAdvanceDay
                        )
                    }

                    composable(AppRoutes.INVENTORY) {
                        InventoryScreen(
                            state = state,
                            onBack = { vm.setPaused(false); navController.popBackStack() },
                            onEquipSlot = vm::selectHotbarSlot,
                            onEatItem = vm::eatItem,
                            onGiftItem = vm::giveGiftToCrush,
                            onSellItem = vm::sellItem
                        )
                    }

                    composable(AppRoutes.SHOP) {
                        ShopScreen(
                            state = state,
                            onBack = { vm.setPaused(false); navController.popBackStack() },
                            onBuySeed = vm::buySeed,
                            onBuySnack = { vm.buySnackOrGift(isGift = false) },
                            onBuyGift = { vm.buySnackOrGift(isGift = true) },
                            onBuyOrEquipOutfit = vm::buyAndEquipOutfit,
                            onSellAllCrops = vm::sellAllCropsFromBag
                        )
                    }

                    composable(AppRoutes.PROFILE) {
                        CharacterProfileScreen(
                            state = state,
                            onBack = { vm.setPaused(false); navController.popBackStack() },
                            onOpenSalonEditor = { navController.navigate(AppRoutes.SALON) }
                        )
                    }

                    composable(AppRoutes.SALON) {
                        CharacterCreatorScreen(
                            initialPlayer = state.playerAppearance,
                            initialCrush = state.crushAppearance,
                            unlockedOutfits = state.unlockedOutfits,
                            isEditInGame = true,
                            onBack = { navController.popBackStack() },
                            onConfirm = { playerApp, crushApp ->
                                vm.updatePlayerAppearance(playerApp)
                                vm.updateCrushAppearance(crushApp)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(AppRoutes.RELATIONSHIP) {
                        RelationshipScreen(
                            state = state,
                            onBack = { vm.setPaused(false); navController.popBackStack() },
                            onStartDate = vm::startDateEvent,
                            onGiveGift = vm::giveGiftToCrush
                        )
                    }

                    composable(AppRoutes.JOURNAL) {
                        JournalScreen(
                            state = state,
                            onBack = { vm.setPaused(false); navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.SETTINGS_MENU) {
                        SettingsScreen(
                            state = state,
                            inGame = false,
                            onBack = { navController.popBackStack() },
                            onUpdateAudio = vm::updateAudioSettings,
                            onSaveNow = vm::saveNow,
                            onReturnMainMenu = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.SETTINGS_GAME) {
                        SettingsScreen(
                            state = state,
                            inGame = true,
                            onBack = { vm.setPaused(false); navController.popBackStack() },
                            onUpdateAudio = vm::updateAudioSettings,
                            onSaveNow = vm::saveNow,
                            onReturnMainMenu = {
                                vm.setPaused(false)
                                navController.navigate(AppRoutes.MAIN_MENU) {
                                    popUpTo(AppRoutes.GAMEPLAY) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
