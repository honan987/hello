package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.game.audio.SoundEngine
import com.example.game.model.*
import com.example.game.npc.CrushSystem
import com.example.game.world.FarmWorldMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.random.Random

data class FloatingParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val kind: ParticleKind,
    val life: Float,
    val maxLife: Float,
    val label: String = ""
)

enum class ParticleKind {
    LEAF, FIREFLY, RAIN, METEOR, HEART, WATER_DROP, HARVEST_ORB, DUST
}

data class GameUiState(
    val isReady: Boolean = false,
    val hasExistingSave: Boolean = false,
    val playerName: String = "An",
    val farmName: String = "Khu Vườn Có Cậu",
    val crushCustomName: String = "Minh",
    val playerAppearance: CharacterAppearance = CharacterCatalog.defaultPlayerAppearance(Gender.FEMALE),
    val crushAppearance: CharacterAppearance = CharacterCatalog.defaultCrushAppearance(Gender.MALE),
    val unlockedOutfits: Set<String> = CharacterCatalog.outfitItems.filter { it.unlockedByDefault }.map { it.id }.toSet(),
    val day: Int = 1,
    val minuteOfDay: Int = 8 * 60 + 30,
    val coins: Int = 220,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val weather: WeatherType = WeatherType.SUNNY,
    val affection: Int = 15,
    val talkedToday: Boolean = false,
    val giftedToday: Boolean = false,
    val completedDates: Set<String> = emptySet(),
    val playerX: Float = 6.5f,
    val playerY: Float = 9.2f,
    val playerDir: Direction = Direction.DOWN,
    val playerAction: CharacterAction = CharacterAction.IDLE,
    val isRunning: Boolean = false,
    val crushX: Float = 24.5f,
    val crushY: Float = 8.5f,
    val crushDir: Direction = Direction.DOWN,
    val crushAction: CharacterAction = CharacterAction.WATER,
    val crushBubble: String? = null,
    val animFrame: Int = 0,
    val blinkOn: Boolean = false,
    val plots: Map<Pair<Int, Int>, FarmPlotState> = emptyMap(),
    val inventory: List<InventoryItem> = emptyList(),
    val selectedSlotIndex: Int = 0,
    val quests: List<QuestItem> = emptyList(),
    val memories: List<MemoryEntry> = emptyList(),
    val particles: List<FloatingParticle> = emptyList(),
    val activeDialogue: ActiveDialogue? = null,
    val toastMessage: String? = null,
    val cloudOffset: Float = 0f,
    val audioSettings: AudioSettings = AudioSettings(),
    val isPaused: Boolean = false
) {
    val currentTimePhase: TimePhase
        get() {
            val h = (minuteOfDay / 60) % 24
            return TimePhase.entries.lastOrNull { h >= it.startHour } ?: TimePhase.NIGHT
        }

    val formattedClock: String
        get() {
            val h = (minuteOfDay / 60) % 24
            val m = minuteOfDay % 60
            return "%02d:%02d".format(h, m)
        }

    val relationshipTier: RelationshipTier
        get() = RelationshipTier.fromAffection(affection)

    val currentZone: WorldZone
        get() = FarmWorldMap.getZoneAt(playerX.toInt(), playerY.toInt())

    val selectedItem: InventoryItem?
        get() = inventory.getOrNull(selectedSlotIndex)

    val crushDisplayName: String
        get() = CrushSystem.crushName(crushAppearance.gender, crushCustomName)
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FarmDatabase.getInstance(application)
    private val repository = FarmRepository(application, db.farmDao())
    val soundEngine = SoundEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var moveDx = 0f
    private var moveDy = 0f
    private var tapTarget: Pair<Float, Float>? = null
    private var actionLockTicks = 0
    private var particleSeq = 1L
    private var gameLoopJob: Job? = null
    private var tickCounter = 0

    init {
        viewModelScope.launch {
            repository.audioSettings.collect { settings ->
                soundEngine.bgmVolume = settings.bgmVolume
                soundEngine.sfxVolume = settings.sfxVolume
                soundEngine.isMuted = settings.isMuted
                _uiState.update { it.copy(audioSettings = settings) }
            }
        }
        viewModelScope.launch {
            repository.hasSaveFlow.collect { hasSave ->
                _uiState.update { it.copy(hasExistingSave = hasSave, isReady = true) }
            }
        }
    }

    fun startMusicIfNeeded() {
        soundEngine.startBgm()
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stopBgm()
    }

    fun setPaused(paused: Boolean) {
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun initializeNewGame(
        playerName: String,
        farmName: String,
        crushGender: Gender,
        crushName: String,
        playerAppearance: CharacterAppearance,
        crushAppearance: CharacterAppearance
    ) {
        val starterPlots = FarmWorldMap.initialFarmPlots.associateWith { (x, y) ->
            FarmPlotState(tileX = x, tileY = y, tilled = (x in 4..5 && y in 12..13), watered = false)
        }
        val starterItems = defaultStarterInventory()
        val starterQuests = defaultQuests()
        val starterMemory = listOf(
            MemoryEntry(
                id = "mem_arrival",
                day = 1,
                titleVi = "Ngày đầu ở ${farmName.ifBlank { "Khu Vườn Có Cậu" }}",
                descriptionVi = "Mùa xuân dịu êm bắt đầu. Hạt giống đầu tiên đã sẵn sàng gieo xuống mảnh đất mới!",
                zoneNameVi = WorldZone.FARM.nameVi,
                isLoveDiary = false
            )
        )
        val defaultUnlocks = CharacterCatalog.outfitItems.filter { it.unlockedByDefault }.map { it.id }.toSet()

        _uiState.value = GameUiState(
            isReady = true,
            hasExistingSave = true,
            playerName = playerName.ifBlank { "An" },
            farmName = farmName.ifBlank { "Khu Vườn Có Cậu" },
            crushCustomName = crushName.ifBlank { if (crushGender == Gender.MALE) "Minh" else "Linh" },
            playerAppearance = playerAppearance,
            crushAppearance = crushAppearance,
            unlockedOutfits = defaultUnlocks,
            day = 1,
            minuteOfDay = 7 * 60 + 30,
            coins = 250,
            energy = 100,
            maxEnergy = 100,
            weather = WeatherType.SUNNY,
            affection = 15,
            plots = starterPlots,
            inventory = starterItems,
            quests = starterQuests,
            memories = starterMemory,
            audioSettings = _uiState.value.audioSettings
        )
        saveNow()
        startGameLoop()
    }

    fun continueSavedGame(onLoaded: (Boolean) -> Unit) {
        viewModelScope.launch {
            val loaded = repository.loadSave()
            if (loaded == null) {
                onLoaded(false)
                return@launch
            }
            val s = loaded.save
            val pApp = repository.decodeAppearance(s.playerAppearanceJson, CharacterCatalog.defaultPlayerAppearance(Gender.FEMALE))
            val cApp = repository.decodeAppearance(s.crushAppearanceJson, CharacterCatalog.defaultCrushAppearance(Gender.MALE))
            val plotMap = loaded.plots.associate { (it.tileX to it.tileY) to FarmPlotState(
                tileX = it.tileX,
                tileY = it.tileY,
                tilled = it.tilled,
                watered = it.watered,
                cropType = CropType.entries.find { c -> c.id == it.cropId },
                daysGrown = it.daysGrown
            ) }.ifEmpty {
                FarmWorldMap.initialFarmPlots.associateWith { (x, y) -> FarmPlotState(x, y) }
            }

            val invItems = rebuildInventoryFromEntities(loaded.inventory)
            val quests = rebuildQuestsFromEntities(loaded.quests)
            val memories = loaded.memories.map {
                MemoryEntry(it.id, it.day, it.titleVi, it.descriptionVi, it.zoneNameVi, it.isLoveDiary)
            }
            val unlocks = s.unlockedOutfitsCsv.split(",").filter { it.isNotBlank() }.toSet()
                .ifEmpty { CharacterCatalog.outfitItems.filter { it.unlockedByDefault }.map { it.id }.toSet() }
            val dates = s.completedDatesCsv.split(",").filter { it.isNotBlank() }.toSet()
            val weather = WeatherType.entries.find { it.name == s.weatherName } ?: WeatherType.SUNNY

            _uiState.value = GameUiState(
                isReady = true,
                hasExistingSave = true,
                playerName = s.playerName,
                farmName = s.farmName,
                crushCustomName = s.crushCustomName,
                playerAppearance = pApp,
                crushAppearance = cApp,
                unlockedOutfits = unlocks,
                day = s.day,
                minuteOfDay = s.minuteOfDay,
                coins = s.coins,
                energy = s.energy,
                maxEnergy = s.maxEnergy,
                weather = weather,
                affection = s.affection,
                talkedToday = s.talkedToday,
                giftedToday = s.giftedToday,
                completedDates = dates,
                playerX = s.playerX,
                playerY = s.playerY,
                plots = plotMap,
                inventory = invItems,
                quests = quests,
                memories = memories,
                audioSettings = _uiState.value.audioSettings
            )
            soundEngine.isRainy = weather == WeatherType.RAINY
            startGameLoop()
            onLoaded(true)
        }
    }

    fun saveNow() {
        val st = _uiState.value
        viewModelScope.launch {
            val saveEntity = SaveStateEntity(
                id = 1,
                playerName = st.playerName,
                farmName = st.farmName,
                crushCustomName = st.crushCustomName,
                day = st.day,
                minuteOfDay = st.minuteOfDay,
                coins = st.coins,
                energy = st.energy,
                maxEnergy = st.maxEnergy,
                weatherName = st.weather.name,
                affection = st.affection,
                talkedToday = st.talkedToday,
                giftedToday = st.giftedToday,
                playerX = st.playerX,
                playerY = st.playerY,
                playerAppearanceJson = repository.encodeAppearance(st.playerAppearance),
                crushAppearanceJson = repository.encodeAppearance(st.crushAppearance),
                unlockedOutfitsCsv = st.unlockedOutfits.joinToString(","),
                completedDatesCsv = st.completedDates.joinToString(",")
            )
            val plotEntities = st.plots.values.map {
                PlotEntity(it.tileX, it.tileY, it.tilled, it.watered, it.cropType?.id, it.daysGrown)
            }
            val invEntities = st.inventory.map { InventoryEntity(it.id, it.quantity) }
            val memEntities = st.memories.map {
                MemoryEntity(it.id, it.day, it.titleVi, it.descriptionVi, it.zoneNameVi, it.isLoveDiary)
            }
            val qEntities = st.quests.map { QuestEntity(it.id, it.progress, it.completed) }
            repository.saveFullGame(saveEntity, plotEntities, invEntities, memEntities, qEntities)
        }
    }

    fun startGameLoop() {
        if (gameLoopJob?.isActive == true) return
        soundEngine.startBgm()
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(50L) // 20 FPS smooth simulation tick
                tickSimulation()
            }
        }
    }

    private fun tickSimulation() {
        val st = _uiState.value
        if (st.isPaused || st.activeDialogue != null) return

        tickCounter++
        val nextFrame = (tickCounter / 4) % 4
        val blinkOn = (tickCounter % 70) in 0..2
        var newX = st.playerX
        var newY = st.playerY
        var newDir = st.playerDir
        var newAction = st.playerAction

        // 1. Movement (D-pad or tap target)
        var dx = moveDx
        var dy = moveDy
        val target = tapTarget
        if (dx == 0f && dy == 0f && target != null) {
            val tdx = target.first - st.playerX
            val tdy = target.second - st.playerY
            val dist = hypot(tdx, tdy)
            if (dist < 0.16f) {
                tapTarget = null
            } else {
                dx = tdx / dist
                dy = tdy / dist
            }
        }

        if (actionLockTicks > 0) {
            actionLockTicks--
            if (actionLockTicks == 0) newAction = CharacterAction.IDLE
        } else if (abs(dx) > 0.05f || abs(dy) > 0.05f) {
            val speed = if (st.isRunning) 0.22f else 0.14f
            val candX = newX + dx * speed
            val candY = newY + dy * speed
            if (FarmWorldMap.isWalkable(candX.toInt(), newY.toInt())) newX = candX
            if (FarmWorldMap.isWalkable(newX.toInt(), candY.toInt())) newY = candY

            newDir = if (abs(dx) > abs(dy)) {
                if (dx < 0) Direction.LEFT else Direction.RIGHT
            } else {
                if (dy < 0) Direction.UP else Direction.DOWN
            }
            newAction = if (st.isRunning) CharacterAction.RUN else CharacterAction.WALK
            if (tickCounter % 7 == 0) soundEngine.playStep()
        } else {
            newAction = CharacterAction.IDLE
        }

        // 2. Advance game clock (1 game minute every 10 ticks = 0.5s; cozy pace!)
        var minuteOfDay = st.minuteOfDay
        var day = st.day
        var weather = st.weather
        if (tickCounter % 10 == 0) {
            minuteOfDay++
            if (minuteOfDay >= 23 * 60 + 50) {
                // Late night automatic gentle rest
                minuteOfDay = 23 * 60 + 50
            }
        }

        // 3. Crush AI Schedule & Movement
        val hour = (minuteOfDay / 60) % 24
        val slot = CrushSystem.currentScheduleFor(hour)
        var cx = st.crushX
        var cy = st.crushY
        var cDir = st.crushDir
        var cAction = slot.activityAction
        val cdx = slot.targetX - cx
        val cdy = slot.targetY - cy
        val cDist = hypot(cdx, cdy)
        if (cDist > 0.2f) {
            val step = 0.09f
            cx += (cdx / cDist) * step
            cy += (cdy / cDist) * step
            cDir = if (abs(cdx) > abs(cdy)) {
                if (cdx < 0) Direction.LEFT else Direction.RIGHT
            } else {
                if (cdy < 0) Direction.UP else Direction.DOWN
            }
            cAction = CharacterAction.WALK
        } else {
            cDir = slot.facing
        }

        // Proximity greeting wave!
        val distToPlayer = hypot(newX - cx, newY - cy)
        var bubble = st.crushBubble
        if (distToPlayer < 2.2f) {
            cAction = CharacterAction.WAVE
            cDir = faceToward(cx, cy, newX, newY)
            if (bubble == null && tickCounter % 120 == 0) {
                bubble = if (st.affection >= 60) "Chào ${st.playerName} ♥" else "Chào cậu!"
            }
        } else if (tickCounter % 80 == 0) {
            bubble = null
        }

        // 4. Environment Particles (falling leaves, fireflies at dusk/night, rain, meteor shower)
        val updatedParticles = st.particles.mapNotNull { p ->
            val nl = p.life + 0.05f
            if (nl >= p.maxLife) null
            else p.copy(x = p.x + p.vx, y = p.y + p.vy, life = nl)
        }.toMutableList()

        if (tickCounter % 8 == 0 && updatedParticles.size < 45) {
            // Leaves
            if (Random.nextFloat() < 0.45f) {
                updatedParticles += FloatingParticle(
                    id = particleSeq++,
                    x = (newX - 7f) + Random.nextFloat() * 14f,
                    y = (newY - 6f) + Random.nextFloat() * 4f,
                    vx = 0.03f + Random.nextFloat() * 0.03f,
                    vy = 0.04f + Random.nextFloat() * 0.02f,
                    kind = ParticleKind.LEAF,
                    life = 0f,
                    maxLife = 4.0f
                )
            }
            // Night fireflies
            if (hour >= 18 || hour < 6) {
                updatedParticles += FloatingParticle(
                    id = particleSeq++,
                    x = (newX - 6f) + Random.nextFloat() * 12f,
                    y = (newY - 5f) + Random.nextFloat() * 10f,
                    vx = (Random.nextFloat() - 0.5f) * 0.04f,
                    vy = (Random.nextFloat() - 0.5f) * 0.04f,
                    kind = ParticleKind.FIREFLY,
                    life = 0f,
                    maxLife = 3.5f
                )
            }
            // Rain drops
            if (weather == WeatherType.RAINY) {
                repeat(3) {
                    updatedParticles += FloatingParticle(
                        id = particleSeq++,
                        x = (newX - 8f) + Random.nextFloat() * 16f,
                        y = (newY - 7f) + Random.nextFloat() * 10f,
                        vx = -0.05f,
                        vy = 0.25f,
                        kind = ParticleKind.RAIN,
                        life = 0f,
                        maxLife = 0.7f
                    )
                }
            }
            // Meteor shower streaks at night
            if (weather == WeatherType.METEOR_SHOWER && hour >= 19 && Random.nextFloat() < 0.35f) {
                updatedParticles += FloatingParticle(
                    id = particleSeq++,
                    x = (newX - 6f) + Random.nextFloat() * 10f,
                    y = newY - 6f,
                    vx = 0.24f,
                    vy = 0.14f,
                    kind = ParticleKind.METEOR,
                    life = 0f,
                    maxLife = 1.1f
                )
            }
        }

        // Auto-clear toast
        val toast = if (tickCounter % 65 == 0) null else st.toastMessage

        _uiState.update {
            it.copy(
                playerX = newX,
                playerY = newY,
                playerDir = newDir,
                playerAction = newAction,
                crushX = cx,
                crushY = cy,
                crushDir = cDir,
                crushAction = cAction,
                crushBubble = bubble,
                minuteOfDay = minuteOfDay,
                day = day,
                weather = weather,
                animFrame = nextFrame,
                blinkOn = blinkOn,
                particles = updatedParticles,
                cloudOffset = (it.cloudOffset + 0.015f) % 40f,
                toastMessage = toast
            )
        }

        if (tickCounter % 240 == 0) saveNow()
    }

    fun setJoystickVector(dx: Float, dy: Float) {
        moveDx = dx
        moveDy = dy
        if (dx != 0f || dy != 0f) tapTarget = null
    }

    fun toggleRunMode() {
        soundEngine.playUiClick()
        _uiState.update { it.copy(isRunning = !it.isRunning) }
    }

    fun selectHotbarSlot(index: Int) {
        soundEngine.playUiClick()
        _uiState.update { it.copy(selectedSlotIndex = index.coerceIn(0, (it.inventory.size - 1).coerceAtLeast(0))) }
    }

    fun onWorldTileTapped(tileX: Int, tileY: Int) {
        val st = _uiState.value
        val distToCrush = hypot(tileX + 0.5f - st.crushX, tileY + 0.5f - st.crushY)
        val playerDistToCrush = hypot(st.playerX - st.crushX, st.playerY - st.crushY)
        if (distToCrush < 1.3f && playerDistToCrush < 2.8f) {
            interactWithCrush()
            return
        }

        val playerDistToTile = hypot(tileX + 0.5f - st.playerX, tileY + 0.5f - st.playerY)
        if (playerDistToTile <= 2.1f) {
            performActionAtTile(tileX, tileY)
        } else {
            // Walk smoothly toward tapped tile
            tapTarget = (tileX + 0.5f) to (tileY + 0.5f)
        }
    }

    fun performPrimaryAction() {
        val st = _uiState.value
        // Check if Crush is nearby first
        if (hypot(st.playerX - st.crushX, st.playerY - st.crushY) < 2.3f) {
            interactWithCrush()
            return
        }
        // Check nearby special landmark prop
        val facingTile = facingTileCoords(st)
        val nearbyProp = FarmWorldMap.props.firstOrNull { prop ->
            val cx = prop.tileX + prop.type.widthTiles * 0.5f
            val cy = prop.tileY + prop.type.heightTiles * 0.5f
            hypot(st.playerX - cx, st.playerY - cy) < 2.4f &&
                prop.type in setOf(
                    PropType.PLAYER_HOUSE, PropType.SHIPPING_BIN,
                    PropType.WELL, PropType.BENCH, PropType.WISHING_CRYSTAL, PropType.MIRROR_POST
                )
        }
        if (nearbyProp != null && st.selectedItem?.toolType == ToolType.HAND) {
            interactWithProp(nearbyProp)
            return
        }
        performActionAtTile(facingTile.first, facingTile.second)
    }

    private fun interactWithProp(prop: WorldProp) {
        val st = _uiState.value
        when (prop.type) {
            PropType.PLAYER_HOUSE -> sleepAndAdvanceDay()
            PropType.SHIPPING_BIN -> sellAllCropsFromBag()
            PropType.WELL -> {
                soundEngine.playWater()
                _uiState.update {
                    it.copy(
                        energy = (it.energy + 25).coerceAtMost(it.maxEnergy),
                        toastMessage = "Uống nước giếng mát lành! +25 Năng lượng"
                    )
                }
            }
            PropType.BENCH -> {
                soundEngine.playUiClick()
                _uiState.update {
                    it.copy(
                        playerAction = CharacterAction.SIT,
                        energy = (it.energy + 15).coerceAtMost(it.maxEnergy),
                        toastMessage = "Ngồi hóng gió nhẹ nhàng... +15 Năng lượng"
                    )
                }
            }
            PropType.WISHING_CRYSTAL -> {
                soundEngine.playHeart()
                _uiState.update {
                    it.copy(
                        affection = (it.affection + 5).coerceAtMost(100),
                        toastMessage = "Đá ước nguyện lấp lánh! +5 Thiện cảm"
                    )
                }
            }
            PropType.MIRROR_POST -> {
                _uiState.update { it.copy(toastMessage = "Mở Hồ sơ nhân vật để thay đổi ngoại hình bất cứ lúc nào!") }
            }
            else -> {}
        }
    }

    private fun performActionAtTile(tx: Int, ty: Int) {
        val st = _uiState.value
        val item = st.selectedItem ?: return
        val plotKey = tx to ty
        val plot = st.plots[plotKey]

        // Check if tapping a landmark directly
        val tappedProp = FarmWorldMap.props.firstOrNull {
            tx in it.tileX until (it.tileX + it.type.widthTiles) &&
                ty in it.tileY until (it.tileY + it.type.heightTiles)
        }
        if (tappedProp != null && tappedProp.type in setOf(
                PropType.PLAYER_HOUSE, PropType.SHIPPING_BIN, PropType.WELL,
                PropType.BENCH, PropType.WISHING_CRYSTAL
            )
        ) {
            interactWithProp(tappedProp)
            return
        }

        if (plot == null) {
            showToast("Hãy chọn ô đất nông trại để canh tác nhé!")
            return
        }

        // Harvest priority if mature!
        if (plot.isMature && (item.toolType == ToolType.HAND || item.category == ItemCategory.TOOL)) {
            harvestPlot(plot)
            return
        }

        when {
            item.toolType == ToolType.HOE -> {
                if (st.energy < 4) {
                    showToast("Hết năng lượng rồi! Hãy ăn nhẹ hoặc về nhà nghỉ ngơi.")
                    return
                }
                if (!plot.tilled) {
                    soundEngine.playHoe()
                    actionLockTicks = 7
                    val newPlots = st.plots.toMutableMap()
                    newPlots[plotKey] = plot.copy(tilled = true)
                    _uiState.update {
                        it.copy(
                            playerAction = CharacterAction.HOE,
                            energy = (it.energy - 4).coerceAtLeast(0),
                            plots = newPlots,
                            quests = advanceQuest(it.quests, "q_till", 1),
                            particles = it.particles + spawnBurst(tx + 0.5f, ty + 0.5f, ParticleKind.DUST, 5),
                            toastMessage = "Đã cuốc đất tơi xốp!"
                        )
                    }
                }
            }
            item.category == ItemCategory.SEED && item.cropType != null -> {
                if (!plot.tilled) {
                    showToast("Cần dùng Cuốc để làm đất trước khi gieo hạt!")
                    return
                }
                if (plot.cropType != null) {
                    showToast("Ô đất này đã có cây đang lớn!")
                    return
                }
                soundEngine.playSeed()
                actionLockTicks = 6
                val newPlots = st.plots.toMutableMap()
                newPlots[plotKey] = plot.copy(cropType = item.cropType, daysGrown = 0)
                val newInv = consumeInventoryItem(st.inventory, item.id, 1)
                _uiState.update {
                    it.copy(
                        playerAction = CharacterAction.HARVEST,
                        plots = newPlots,
                        inventory = newInv,
                        quests = advanceQuest(it.quests, "q_plant", 1),
                        toastMessage = "Đã gieo hạt ${item.cropType.nameVi}!"
                    )
                }
            }
            item.toolType == ToolType.WATERING_CAN -> {
                if (st.energy < 3) {
                    showToast("Mệt quá rồi! Hãy nghỉ ngơi hoặc ăn một chút.")
                    return
                }
                if (!plot.tilled) {
                    showToast("Cuốc đất trước rồi hãy tưới nước nha!")
                    return
                }
                soundEngine.playWater()
                actionLockTicks = 8
                val newPlots = st.plots.toMutableMap()
                newPlots[plotKey] = plot.copy(watered = true)
                _uiState.update {
                    it.copy(
                        playerAction = CharacterAction.WATER,
                        energy = (it.energy - 3).coerceAtLeast(0),
                        plots = newPlots,
                        quests = advanceQuest(it.quests, "q_water", 1),
                        particles = it.particles + spawnBurst(tx + 0.5f, ty + 0.5f, ParticleKind.WATER_DROP, 6),
                        toastMessage = "Tưới nước mát cho cây!"
                    )
                }
            }
            item.toolType == ToolType.HAND -> {
                if (plot.isMature) {
                    harvestPlot(plot)
                } else if (plot.cropType != null) {
                    val left = (plot.cropType.daysToGrow - plot.daysGrown).coerceAtLeast(1)
                    showToast("${plot.cropType.nameVi}: còn $left ngày nữa thu hoạch.")
                }
            }
            item.category == ItemCategory.SNACK -> {
                eatItem(item)
            }
        }
    }

    private fun harvestPlot(plot: FarmPlotState) {
        val crop = plot.cropType ?: return
        val st = _uiState.value
        soundEngine.playHarvest()
        actionLockTicks = 7
        val newPlots = st.plots.toMutableMap()
        newPlots[plot.tileX to plot.tileY] = plot.copy(cropType = null, daysGrown = 0, watered = false)
        val harvestedItem = InventoryItem(
            id = "crop_${crop.id}",
            nameVi = crop.nameVi,
            category = ItemCategory.CROP,
            quantity = 1,
            cropType = crop,
            sellPrice = crop.sellPrice,
            energyRestore = crop.energyRestore,
            affectionPoints = if (crop.crushLikedLevel == 2) 14 else 8,
            descriptionVi = crop.descriptionVi
        )
        val newInv = addInventoryItem(st.inventory, harvestedItem)
        _uiState.update {
            it.copy(
                playerAction = CharacterAction.HARVEST,
                plots = newPlots,
                inventory = newInv,
                quests = advanceQuest(it.quests, "q_harvest", 1),
                particles = it.particles + spawnBurst(plot.tileX + 0.5f, plot.tileY + 0.5f, ParticleKind.HARVEST_ORB, 6, crop.nameVi),
                toastMessage = "Thu hoạch ${crop.nameVi}! (+1 vào túi)"
            )
        }
        saveNow()
    }

    fun interactWithCrush() {
        val st = _uiState.value
        val item = st.selectedItem
        if (item != null && (item.toolType == ToolType.GIFT || item.category == ItemCategory.CROP || item.category == ItemCategory.GIFT_ITEM)) {
            if (item.toolType == ToolType.GIFT) {
                // Find first giftable crop or gift item in bag
                val giftable = st.inventory.firstOrNull { it.category == ItemCategory.CROP || it.category == ItemCategory.GIFT_ITEM }
                if (giftable != null) {
                    giveGiftToCrush(giftable)
                    return
                }
            } else if (item.category == ItemCategory.CROP || item.category == ItemCategory.GIFT_ITEM) {
                giveGiftToCrush(item)
                return
            }
        }

        soundEngine.playDialogue()
        val dialogue = CrushSystem.buildDialogue(
            crushName = st.crushDisplayName,
            playerName = st.playerName,
            tier = st.relationshipTier,
            timePhase = st.currentTimePhase,
            weather = st.weather,
            zone = st.currentZone,
            day = st.day
        )
        _uiState.update {
            it.copy(
                activeDialogue = dialogue,
                crushDir = faceToward(it.crushX, it.crushY, it.playerX, it.playerY)
            )
        }
    }

    fun selectDialogueOption(option: DialogueOption) {
        val st = _uiState.value
        val gain = if (!st.talkedToday) option.affectionDelta else (option.affectionDelta / 2).coerceAtLeast(1)
        val newAffection = (st.affection + gain).coerceIn(0, 100)
        soundEngine.playHeart()

        val updatedMemories = if (option.memoryTitleVi != null) {
            val mem = MemoryEntry(
                id = "mem_${st.day}_${System.currentTimeMillis()}",
                day = st.day,
                titleVi = option.memoryTitleVi,
                descriptionVi = option.responseVi,
                zoneNameVi = st.currentZone.nameVi,
                isLoveDiary = true
            )
            listOf(mem) + st.memories
        } else st.memories

        _uiState.update {
            it.copy(
                affection = newAffection,
                talkedToday = true,
                memories = updatedMemories,
                quests = advanceQuest(it.quests, "q_talk", 1),
                particles = it.particles + spawnBurst(it.crushX, it.crushY - 0.5f, ParticleKind.HEART, 5),
                activeDialogue = ActiveDialogue(
                    speakerName = st.crushDisplayName,
                    textVi = option.responseVi,
                    expression = option.crushExpression,
                    options = emptyList(),
                    canCloseDirectly = true
                )
            )
        }
        saveNow()
    }

    fun giveGiftToCrush(item: InventoryItem) {
        val st = _uiState.value
        if (st.giftedToday) {
            showToast("Hôm nay cậu đã tặng quà cho ${st.crushDisplayName} rồi, mai tặng tiếp nhé!")
            return
        }
        val (reaction, delta, exp) = CrushSystem.evaluateGiftReaction(st.crushDisplayName, item)
        soundEngine.playHeart()
        actionLockTicks = 8
        val newInv = consumeInventoryItem(st.inventory, item.id, 1)
        val mem = MemoryEntry(
            id = "gift_${st.day}_${item.id}",
            day = st.day,
            titleVi = "Tặng ${item.nameVi} cho ${st.crushDisplayName}",
            descriptionVi = reaction,
            zoneNameVi = st.currentZone.nameVi,
            isLoveDiary = true
        )
        _uiState.update {
            it.copy(
                playerAction = CharacterAction.GIFT,
                affection = (it.affection + delta).coerceIn(0, 100),
                giftedToday = true,
                inventory = newInv,
                memories = listOf(mem) + it.memories,
                quests = advanceQuest(it.quests, "q_gift", 1),
                particles = it.particles + spawnBurst(it.crushX, it.crushY - 0.6f, ParticleKind.HEART, 7),
                activeDialogue = ActiveDialogue(
                    speakerName = st.crushDisplayName,
                    textVi = reaction,
                    expression = exp,
                    canCloseDirectly = true
                )
            )
        }
        saveNow()
    }

    fun startDateEvent(dateId: String) {
        val st = _uiState.value
        soundEngine.isDateMode = true
        soundEngine.playHeart()
        val dialogue = CrushSystem.buildDateEvent(dateId, st.crushDisplayName, st.playerName, st.day)
        _uiState.update {
            it.copy(
                completedDates = it.completedDates + dateId,
                activeDialogue = dialogue
            )
        }
    }

    fun closeDialogue() {
        soundEngine.isDateMode = false
        _uiState.update { it.copy(activeDialogue = null) }
    }

    fun eatItem(item: InventoryItem) {
        if (item.energyRestore <= 0) return
        val st = _uiState.value
        soundEngine.playHarvest()
        val newInv = consumeInventoryItem(st.inventory, item.id, 1)
        _uiState.update {
            it.copy(
                energy = (it.energy + item.energyRestore).coerceAtMost(it.maxEnergy),
                inventory = newInv,
                toastMessage = "Thưởng thức ${item.nameVi}! +${item.energyRestore} Năng lượng"
            )
        }
    }

    fun sellItem(item: InventoryItem) {
        if (item.sellPrice <= 0 || item.quantity <= 0) return
        val st = _uiState.value
        soundEngine.playCoin()
        val newInv = consumeInventoryItem(st.inventory, item.id, 1)
        _uiState.update {
            it.copy(
                coins = it.coins + item.sellPrice,
                inventory = newInv,
                toastMessage = "Đã bán ${item.nameVi} (+${item.sellPrice} xu)"
            )
        }
        saveNow()
    }

    fun sellAllCropsFromBag() {
        val st = _uiState.value
        val crops = st.inventory.filter { it.category == ItemCategory.CROP && it.quantity > 0 }
        if (crops.isEmpty()) {
            showToast("Túi đồ chưa có nông sản để bán!")
            return
        }
        val earned = crops.sumOf { it.sellPrice * it.quantity }
        val newInv = st.inventory.filterNot { it.category == ItemCategory.CROP }
        soundEngine.playCoin()
        _uiState.update {
            it.copy(
                coins = it.coins + earned,
                inventory = newInv,
                toastMessage = "Thùng nông sản: Thu về +$earned xu!"
            )
        }
        saveNow()
    }

    fun buySeed(crop: CropType) {
        val st = _uiState.value
        if (st.coins < crop.seedPrice) {
            showToast("Chưa đủ xu để mua hạt giống ${crop.nameVi}!")
            return
        }
        soundEngine.playCoin()
        val seedItem = InventoryItem(
            id = "seed_${crop.id}",
            nameVi = "Hạt ${crop.nameVi}",
            category = ItemCategory.SEED,
            quantity = 1,
            cropType = crop,
            descriptionVi = "Trồng ${crop.daysToGrow} ngày • Bán ${crop.sellPrice} xu"
        )
        _uiState.update {
            it.copy(
                coins = it.coins - crop.seedPrice,
                inventory = addInventoryItem(it.inventory, seedItem),
                toastMessage = "Đã mua Hạt ${crop.nameVi} (-${crop.seedPrice} xu)"
            )
        }
        saveNow()
    }

    fun buySnackOrGift(isGift: Boolean) {
        val st = _uiState.value
        val price = if (isGift) 75 else 35
        if (st.coins < price) {
            showToast("Không đủ xu rồi!")
            return
        }
        soundEngine.playCoin()
        val item = if (isGift) {
            InventoryItem(
                id = "gift_bouquet",
                nameVi = "Bó hoa đồng nội",
                category = ItemCategory.GIFT_ITEM,
                quantity = 1,
                sellPrice = 40,
                affectionPoints = 12,
                descriptionVi = "Bó hoa thơm ngát dành tặng người thương."
            )
        } else {
            InventoryItem(
                id = "snack_cake",
                nameVi = "Bánh mật ong",
                category = ItemCategory.SNACK,
                quantity = 1,
                sellPrice = 15,
                energyRestore = 40,
                descriptionVi = "Hồi phục +40 Năng lượng ngay lập tức."
            )
        }
        _uiState.update {
            it.copy(
                coins = it.coins - price,
                inventory = addInventoryItem(it.inventory, item),
                toastMessage = "Đã mua ${item.nameVi}!"
            )
        }
        saveNow()
    }

    fun buyAndEquipOutfit(outfit: OutfitItemDef) {
        val st = _uiState.value
        if (!st.unlockedOutfits.contains(outfit.id)) {
            if (st.coins < outfit.price) {
                showToast("Cần ${outfit.price} xu để mở khóa ${outfit.nameVi}!")
                return
            }
            soundEngine.playCoin()
            val updatedApp = applyOutfitToAppearance(st.playerAppearance, outfit)
            _uiState.update {
                it.copy(
                    coins = it.coins - outfit.price,
                    unlockedOutfits = it.unlockedOutfits + outfit.id,
                    playerAppearance = updatedApp,
                    toastMessage = "Đã mua & mặc ${outfit.nameVi}!"
                )
            }
        } else {
            soundEngine.playUiClick()
            _uiState.update {
                it.copy(
                    playerAppearance = applyOutfitToAppearance(it.playerAppearance, outfit),
                    toastMessage = "Đã thay ${outfit.nameVi}!"
                )
            }
        }
        saveNow()
    }

    fun updatePlayerAppearance(newApp: CharacterAppearance) {
        _uiState.update { it.copy(playerAppearance = newApp) }
        saveNow()
    }

    fun updateCrushAppearance(newApp: CharacterAppearance) {
        _uiState.update { it.copy(crushAppearance = newApp) }
        saveNow()
    }

    fun sleepAndAdvanceDay() {
        val st = _uiState.value
        val wasRainy = st.weather == WeatherType.RAINY
        val grownPlots = st.plots.mapValues { (_, plot) ->
            val canGrow = plot.cropType != null && (plot.watered || wasRainy)
            plot.copy(
                daysGrown = if (canGrow) (plot.daysGrown + 1).coerceAtMost(plot.cropType!!.daysToGrow) else plot.daysGrown,
                watered = false
            )
        }

        val nextWeather = if (wasRainy && Random.nextFloat() < 0.65f) {
            WeatherType.RAINBOW
        } else {
            when (Random.nextInt(10)) {
                0, 1 -> WeatherType.RAINY
                2, 3 -> WeatherType.CLOUDY
                4 -> WeatherType.FOGGY
                5 -> WeatherType.METEOR_SHOWER
                else -> WeatherType.SUNNY
            }
        }
        soundEngine.isRainy = nextWeather == WeatherType.RAINY
        soundEngine.playHarvest()

        _uiState.update {
            it.copy(
                day = it.day + 1,
                minuteOfDay = 6 * 60 + 30,
                energy = it.maxEnergy,
                weather = nextWeather,
                talkedToday = false,
                giftedToday = false,
                plots = grownPlots,
                playerX = 6.5f,
                playerY = 9.0f,
                toastMessage = "Chào ngày mới ${it.day + 1}! Thời tiết: ${nextWeather.labelVi}"
            )
        }
        saveNow()
    }

    fun updateAudioSettings(bgm: Float? = null, sfx: Float? = null, muted: Boolean? = null) {
        val cur = _uiState.value.audioSettings
        val next = AudioSettings(
            bgmVolume = bgm ?: cur.bgmVolume,
            sfxVolume = sfx ?: cur.sfxVolume,
            isMuted = muted ?: cur.isMuted
        )
        viewModelScope.launch { repository.updateAudioSettings(next) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    private fun applyOutfitToAppearance(app: CharacterAppearance, outfit: OutfitItemDef): CharacterAppearance =
        when (outfit.category) {
            OutfitCategory.TOP -> app.copy(topId = outfit.id, topColorIndex = outfit.defaultColorIndex)
            OutfitCategory.BOTTOM -> app.copy(bottomId = outfit.id, bottomColorIndex = outfit.defaultColorIndex)
            OutfitCategory.SHOES -> app.copy(shoesId = outfit.id)
            OutfitCategory.HAT -> app.copy(hatId = outfit.id)
            OutfitCategory.ACCESSORY -> app.copy(accessoryId = outfit.id)
        }

    private fun facingTileCoords(st: GameUiState): Pair<Int, Int> {
        val px = st.playerX.toInt()
        val py = st.playerY.toInt()
        return when (st.playerDir) {
            Direction.DOWN -> px to (py + 1)
            Direction.UP -> px to (py - 1)
            Direction.LEFT -> (px - 1) to py
            Direction.RIGHT -> (px + 1) to py
        }
    }

    private fun faceToward(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val dx = x2 - x1
        val dy = y2 - y1
        return if (abs(dx) > abs(dy)) {
            if (dx < 0) Direction.LEFT else Direction.RIGHT
        } else {
            if (dy < 0) Direction.UP else Direction.DOWN
        }
    }

    private fun spawnBurst(x: Float, y: Float, kind: ParticleKind, count: Int, label: String = ""): List<FloatingParticle> {
        return List(count) {
            FloatingParticle(
                id = particleSeq++,
                x = x + (Random.nextFloat() - 0.5f) * 0.4f,
                y = y + (Random.nextFloat() - 0.5f) * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.08f,
                vy = -0.05f - Random.nextFloat() * 0.06f,
                kind = kind,
                life = 0f,
                maxLife = 0.8f,
                label = label
            )
        }
    }

    private fun addInventoryItem(list: List<InventoryItem>, item: InventoryItem): List<InventoryItem> {
        val idx = list.indexOfFirst { it.id == item.id }
        return if (idx >= 0) {
            list.toMutableList().apply {
                val cur = this[idx]
                this[idx] = cur.copy(quantity = cur.quantity + item.quantity)
            }
        } else {
            list + item
        }
    }

    private fun consumeInventoryItem(list: List<InventoryItem>, itemId: String, amount: Int): List<InventoryItem> {
        return list.mapNotNull { item ->
            if (item.id != itemId) item
            else if (item.category == ItemCategory.TOOL) item
            else {
                val rem = item.quantity - amount
                if (rem <= 0) null else item.copy(quantity = rem)
            }
        }
    }

    private fun advanceQuest(quests: List<QuestItem>, questId: String, delta: Int): List<QuestItem> {
        var bonusCoins = 0
        var bonusAffection = 0
        val updated = quests.map { q ->
            if (q.id == questId && !q.completed) {
                val nextProg = (q.progress + delta).coerceAtMost(q.target)
                val done = nextProg >= q.target
                if (done) {
                    bonusCoins += q.rewardCoins
                    bonusAffection += q.rewardAffection
                }
                q.copy(progress = nextProg, completed = done)
            } else q
        }
        if (bonusCoins > 0 || bonusAffection > 0) {
            _uiState.update {
                it.copy(
                    coins = it.coins + bonusCoins,
                    affection = (it.affection + bonusAffection).coerceAtMost(100),
                    toastMessage = "Hoàn thành nhiệm vụ! +$bonusCoins xu, +$bonusAffection thiện cảm"
                )
            }
        }
        return updated
    }

    private fun defaultStarterInventory(): List<InventoryItem> = listOf(
        InventoryItem("tool_hoe", "Cuốc đất", ItemCategory.TOOL, 1, toolType = ToolType.HOE, descriptionVi = "Dùng để cuốc ô đất nông trại"),
        InventoryItem("tool_water", "Bình tưới", ItemCategory.TOOL, 1, toolType = ToolType.WATERING_CAN, descriptionVi = "Tưới nước giúp cây lớn qua ngày"),
        InventoryItem("tool_hand", "Thu hoạch", ItemCategory.TOOL, 1, toolType = ToolType.HAND, descriptionVi = "Thu hoạch cây chín & tương tác"),
        InventoryItem("tool_gift", "Tặng quà", ItemCategory.TOOL, 1, toolType = ToolType.GIFT, descriptionVi = "Tặng quà cho người thương"),
        InventoryItem("seed_turnip", "Hạt Củ cải", ItemCategory.SEED, 6, cropType = CropType.TURNIP, descriptionVi = "Lớn trong 2 ngày"),
        InventoryItem("seed_strawberry", "Hạt Dâu tây", ItemCategory.SEED, 4, cropType = CropType.STRAWBERRY, descriptionVi = "Món Crush cực thích!"),
        InventoryItem("snack_cake", "Bánh mật ong", ItemCategory.SNACK, 2, sellPrice = 15, energyRestore = 40, descriptionVi = "Hồi +40 Năng lượng")
    )

    private fun defaultQuests(): List<QuestItem> = listOf(
        QuestItem("q_till", "Khởi đầu mùa vụ", "Cuốc 4 ô đất trong nông trại", 0, 4, 60, 4),
        QuestItem("q_plant", "Gieo mầm hy vọng", "Gieo 4 hạt giống bất kỳ", 0, 4, 80, 5),
        QuestItem("q_water", "Chăm chút từng giọt", "Tưới nước cho 4 ô đất", 0, 4, 70, 5),
        QuestItem("q_harvest", "Mùa thu hoạch đầu tiên", "Thu hoạch 3 nông sản chín", 0, 3, 120, 8),
        QuestItem("q_talk", "Trò chuyện cùng Crush", "Nói chuyện thân thiện 2 lần", 0, 2, 50, 10),
        QuestItem("q_gift", "Món quà chân thành", "Tặng 1 món quà cho Crush", 0, 1, 90, 12)
    )

    private fun rebuildInventoryFromEntities(entities: List<InventoryEntity>): List<InventoryItem> {
        if (entities.isEmpty()) return defaultStarterInventory()
        val startersById = defaultStarterInventory().associateBy { it.id }
        return entities.mapNotNull { ent ->
            startersById[ent.itemId]?.copy(quantity = ent.quantity) ?: when {
                ent.itemId.startsWith("seed_") -> {
                    val crop = CropType.entries.find { "seed_${it.id}" == ent.itemId } ?: return@mapNotNull null
                    InventoryItem(ent.itemId, "Hạt ${crop.nameVi}", ItemCategory.SEED, ent.quantity, cropType = crop)
                }
                ent.itemId.startsWith("crop_") -> {
                    val crop = CropType.entries.find { "crop_${it.id}" == ent.itemId } ?: return@mapNotNull null
                    InventoryItem(ent.itemId, crop.nameVi, ItemCategory.CROP, ent.quantity, cropType = crop, sellPrice = crop.sellPrice, energyRestore = crop.energyRestore)
                }
                ent.itemId == "gift_bouquet" -> InventoryItem("gift_bouquet", "Bó hoa đồng nội", ItemCategory.GIFT_ITEM, ent.quantity, sellPrice = 40, affectionPoints = 12)
                else -> null
            }
        }.ifEmpty { defaultStarterInventory() }
    }

    private fun rebuildQuestsFromEntities(entities: List<QuestEntity>): List<QuestItem> {
        val defaults = defaultQuests()
        if (entities.isEmpty()) return defaults
        val map = entities.associateBy { it.id }
        return defaults.map { q ->
            val saved = map[q.id]
            if (saved != null) q.copy(progress = saved.progress, completed = saved.completed) else q
        }
    }
}
