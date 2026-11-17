package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import com.example.game.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "farm_settings")

data class AudioSettings(
    val bgmVolume: Float = 0.55f,
    val sfxVolume: Float = 0.75f,
    val isMuted: Boolean = false
)

@Entity(tableName = "player_save")
data class SaveStateEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String,
    val farmName: String,
    val crushCustomName: String,
    val day: Int,
    val minuteOfDay: Int,
    val coins: Int,
    val energy: Int,
    val maxEnergy: Int,
    val weatherName: String,
    val affection: Int,
    val talkedToday: Boolean,
    val giftedToday: Boolean,
    val playerX: Float,
    val playerY: Float,
    val playerAppearanceJson: String,
    val crushAppearanceJson: String,
    val unlockedOutfitsCsv: String,
    val completedDatesCsv: String
)

@Entity(tableName = "farm_plots", primaryKeys = ["tileX", "tileY"])
data class PlotEntity(
    val tileX: Int,
    val tileY: Int,
    val tilled: Boolean,
    val watered: Boolean,
    val cropId: String?,
    val daysGrown: Int
)

@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val day: Int,
    val titleVi: String,
    val descriptionVi: String,
    val zoneNameVi: String,
    val isLoveDiary: Boolean
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val progress: Int,
    val completed: Boolean
)

@Dao
interface FarmDao {
    @Query("SELECT * FROM player_save WHERE id = 1")
    fun observeSave(): Flow<SaveStateEntity?>

    @Query("SELECT * FROM player_save WHERE id = 1")
    suspend fun getSaveOnce(): SaveStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSave(save: SaveStateEntity)

    @Query("SELECT * FROM farm_plots")
    suspend fun getPlotsOnce(): List<PlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlots(plots: List<PlotEntity>)

    @Query("DELETE FROM farm_plots")
    suspend fun clearPlots()

    @Query("SELECT * FROM inventory_items")
    suspend fun getInventoryOnce(): List<InventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventory(items: List<InventoryEntity>)

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()

    @Query("SELECT * FROM memories ORDER BY day DESC")
    suspend fun getMemoriesOnce(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemories(memories: List<MemoryEntity>)

    @Query("DELETE FROM memories")
    suspend fun clearMemories()

    @Query("SELECT * FROM quests")
    suspend fun getQuestsOnce(): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuests(quests: List<QuestEntity>)

    @Query("DELETE FROM quests")
    suspend fun clearQuests()
}

@Database(
    entities = [
        SaveStateEntity::class,
        PlotEntity::class,
        InventoryEntity::class,
        MemoryEntity::class,
        QuestEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FarmDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    companion object {
        @Volatile private var instance: FarmDatabase? = null

        fun getInstance(context: Context): FarmDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    "our_little_farm.db"
                ).build().also { instance = it }
            }
    }
}

class FarmRepository(
    private val context: Context,
    private val dao: FarmDao
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val appearanceAdapter = moshi.adapter(CharacterAppearance::class.java)

    private val KEY_BGM = floatPreferencesKey("bgm_volume")
    private val KEY_SFX = floatPreferencesKey("sfx_volume")
    private val KEY_MUTED = booleanPreferencesKey("is_muted")

    val audioSettings: Flow<AudioSettings> = context.settingsDataStore.data.map { prefs ->
        AudioSettings(
            bgmVolume = prefs[KEY_BGM] ?: 0.55f,
            sfxVolume = prefs[KEY_SFX] ?: 0.75f,
            isMuted = prefs[KEY_MUTED] ?: false
        )
    }

    val hasSaveFlow: Flow<Boolean> = dao.observeSave().map { it != null }

    suspend fun updateAudioSettings(settings: AudioSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_BGM] = settings.bgmVolume
            prefs[KEY_SFX] = settings.sfxVolume
            prefs[KEY_MUTED] = settings.isMuted
        }
    }

    fun encodeAppearance(app: CharacterAppearance): String =
        appearanceAdapter.toJson(app)

    fun decodeAppearance(json: String, fallback: CharacterAppearance): CharacterAppearance =
        runCatching { appearanceAdapter.fromJson(json) }.getOrNull() ?: fallback

    suspend fun saveFullGame(
        save: SaveStateEntity,
        plots: List<PlotEntity>,
        inventory: List<InventoryEntity>,
        memories: List<MemoryEntity>,
        quests: List<QuestEntity>
    ) {
        dao.upsertSave(save)
        dao.clearPlots()
        dao.upsertPlots(plots)
        dao.clearInventory()
        dao.upsertInventory(inventory)
        dao.clearMemories()
        dao.upsertMemories(memories)
        dao.clearQuests()
        dao.upsertQuests(quests)
    }

    suspend fun loadSave(): LoadedGameData? {
        val save = dao.getSaveOnce() ?: return null
        return LoadedGameData(
            save = save,
            plots = dao.getPlotsOnce(),
            inventory = dao.getInventoryOnce(),
            memories = dao.getMemoriesOnce(),
            quests = dao.getQuestsOnce()
        )
    }
}

data class LoadedGameData(
    val save: SaveStateEntity,
    val plots: List<PlotEntity>,
    val inventory: List<InventoryEntity>,
    val memories: List<MemoryEntity>,
    val quests: List<QuestEntity>
)
