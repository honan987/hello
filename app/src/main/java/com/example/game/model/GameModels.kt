package com.example.game.model

enum class Gender(val labelVi: String) {
    MALE("Nam"),
    FEMALE("Nữ")
}

enum class Direction {
    DOWN, UP, LEFT, RIGHT
}

enum class CharacterAction(val labelVi: String) {
    IDLE("Đứng yên"),
    WALK("Đi bộ"),
    RUN("Chạy"),
    HOE("Cuốc đất"),
    WATER("Tưới cây"),
    HARVEST("Thu hoạch"),
    GIFT("Tặng quà"),
    INTERACT("Tương tác"),
    SIT("Ngồi nghỉ"),
    SLEEP("Ngủ"),
    WAVE("Vẫy tay"),
    HAPPY("Vui vẻ"),
    SHY("Ngại ngùng"),
    SAD("Buồn"),
    LOVE("Yêu thương")
}

enum class Expression(val labelVi: String) {
    NEUTRAL("Bình thường"),
    HAPPY("Vui vẻ"),
    SHY("Ngại ngùng"),
    SAD("Buồn"),
    SURPRISED("Bất ngờ"),
    LOVE("Hạnh phúc")
}

enum class EyeStyle(val labelVi: String) {
    ROUND("Mắt tròn"),
    SMILE("Mắt cười"),
    SPARKLE("Mắt long lanh"),
    GENTLE("Mắt dịu dàng")
}

enum class EyebrowStyle(val labelVi: String) {
    NATURAL("Tự nhiên"),
    ARCHED("Thanh tú"),
    SOFT("Mềm mại"),
    PLAYFUL("Tinh nghịch")
}

data class HairStyleDef(
    val id: String,
    val nameVi: String,
    val gender: Gender?, // null means unisex, though we provide 12 per gender
    val shapeKey: String
)

enum class OutfitCategory(val labelVi: String) {
    TOP("Áo"),
    BOTTOM("Quần & Váy"),
    SHOES("Giày"),
    HAT("Mũ"),
    ACCESSORY("Phụ kiện")
}

data class OutfitItemDef(
    val id: String,
    val nameVi: String,
    val category: OutfitCategory,
    val price: Int,
    val unlockedByDefault: Boolean,
    val defaultColorIndex: Int,
    val styleKey: String
)

data class CharacterAppearance(
    val gender: Gender = Gender.FEMALE,
    val skinToneIndex: Int = 1,
    val eyeStyle: EyeStyle = EyeStyle.SPARKLE,
    val eyeColorIndex: Int = 1,
    val eyebrowStyle: EyebrowStyle = EyebrowStyle.SOFT,
    val expression: Expression = Expression.NEUTRAL,
    val hairStyleId: String = "f_layer",
    val hairColorIndex: Int = 1,
    val topId: String = "top_shirt_white",
    val topColorIndex: Int = 0,
    val bottomId: String = "bot_skirt_floral",
    val bottomColorIndex: Int = 3,
    val shoesId: String = "shoe_farm",
    val hatId: String = "hat_none",
    val accessoryId: String = "acc_none"
)

object CharacterCatalog {
    val maleHairStyles = listOf(
        HairStyleDef("m_short", "Tóc ngắn", Gender.MALE, "short"),
        HairStyleDef("m_bangs", "Tóc mái", Gender.MALE, "bangs"),
        HairStyleDef("m_curly", "Tóc xoăn", Gender.MALE, "curly"),
        HairStyleDef("m_layer", "Tóc layer", Gender.MALE, "layer"),
        HairStyleDef("m_parted", "Tóc rẽ ngôi", Gender.MALE, "parted"),
        HairStyleDef("m_long", "Tóc dài", Gender.MALE, "long_male"),
        HairStyleDef("m_tied_high", "Tóc buộc cao", Gender.MALE, "ponytail_high"),
        HairStyleDef("m_tied_low", "Tóc buộc thấp", Gender.MALE, "ponytail_low"),
        HairStyleDef("m_undercut", "Tóc undercut", Gender.MALE, "undercut"),
        HairStyleDef("m_two_part", "Tóc hai mái", Gender.MALE, "two_part"),
        HairStyleDef("m_k_layer", "Layer Hàn Quốc", Gender.MALE, "k_layer"),
        HairStyleDef("m_anime", "Tóc Anime", Gender.MALE, "anime")
    )

    val femaleHairStyles = listOf(
        HairStyleDef("f_long_straight", "Tóc dài thẳng", Gender.FEMALE, "long_straight"),
        HairStyleDef("f_curly", "Tóc xoăn", Gender.FEMALE, "curly_long"),
        HairStyleDef("f_bob", "Tóc bob", Gender.FEMALE, "bob"),
        HairStyleDef("f_flat_bangs", "Tóc mái bằng", Gender.FEMALE, "flat_bangs"),
        HairStyleDef("f_ponytail", "Đuôi ngựa", Gender.FEMALE, "ponytail_high"),
        HairStyleDef("f_bun", "Tóc búi", Gender.FEMALE, "bun"),
        HairStyleDef("f_braid", "Tóc tết", Gender.FEMALE, "braid"),
        HairStyleDef("f_layer", "Layer Hàn Quốc", Gender.FEMALE, "k_layer_long"),
        HairStyleDef("f_tied_low", "Tóc buộc thấp", Gender.FEMALE, "ponytail_low"),
        HairStyleDef("f_two_part", "Tóc hai mái", Gender.FEMALE, "two_part_long"),
        HairStyleDef("f_twin_bun", "Tóc búi đôi", Gender.FEMALE, "twin_bun"),
        HairStyleDef("f_anime", "Tóc Anime", Gender.FEMALE, "anime_long")
    )

    fun hairStylesFor(gender: Gender): List<HairStyleDef> =
        if (gender == Gender.MALE) maleHairStyles else femaleHairStyles

    val outfitItems = listOf(
        // Tops (unlocked + shop)
        OutfitItemDef("top_shirt_white", "Áo sơ mi trắng", OutfitCategory.TOP, 0, true, 0, "shirt"),
        OutfitItemDef("top_sweater_pastel", "Áo len pastel", OutfitCategory.TOP, 0, true, 2, "sweater"),
        OutfitItemDef("top_tee_sunny", "Áo thun nông trại", OutfitCategory.TOP, 0, true, 4, "tee"),
        OutfitItemDef("top_hoodie_cute", "Áo hoodie dễ thương", OutfitCategory.TOP, 0, true, 5, "hoodie"),
        OutfitItemDef("top_picnic_vest", "Áo dã ngoại", OutfitCategory.TOP, 0, true, 6, "vest"),
        OutfitItemDef("top_pajama", "Áo ngủ sao trời", OutfitCategory.TOP, 0, true, 7, "pajama"),
        OutfitItemDef("top_date_coat", "Áo hẹn hò lãng mạn", OutfitCategory.TOP, 180, false, 3, "coat"),
        OutfitItemDef("top_festival_hanbok", "Áo lễ hội hoa", OutfitCategory.TOP, 240, false, 8, "festival"),
        OutfitItemDef("top_winter_knit", "Áo len mùa đông", OutfitCategory.TOP, 160, false, 9, "winter"),

        // Bottoms & Dresses
        OutfitItemDef("bot_jeans", "Quần jeans xanh", OutfitCategory.BOTTOM, 0, true, 1, "jeans"),
        OutfitItemDef("bot_overalls", "Quần yếm nông dân", OutfitCategory.BOTTOM, 0, true, 6, "overalls"),
        OutfitItemDef("bot_skirt_floral", "Váy hoa mùa xuân", OutfitCategory.BOTTOM, 0, true, 3, "skirt_floral"),
        OutfitItemDef("bot_skirt_casual", "Váy casual xếp ly", OutfitCategory.BOTTOM, 0, true, 4, "skirt_casual"),
        OutfitItemDef("bot_shorts", "Quần short dã ngoại", OutfitCategory.BOTTOM, 0, true, 5, "shorts"),
        OutfitItemDef("bot_pajama_pants", "Quần ngủ mềm", OutfitCategory.BOTTOM, 0, true, 7, "pajama_pants"),
        OutfitItemDef("bot_date_outfit", "Váy/Quần hẹn hò", OutfitCategory.BOTTOM, 160, false, 8, "date_bot"),

        // Shoes
        OutfitItemDef("shoe_farm", "Giày nông trại", OutfitCategory.SHOES, 0, true, 0, "boots"),
        OutfitItemDef("shoe_sneaker", "Giày sneaker pastel", OutfitCategory.SHOES, 0, true, 1, "sneaker"),
        OutfitItemDef("shoe_maryjane", "Giày búp bê/Da nâu", OutfitCategory.SHOES, 90, false, 2, "dress_shoe"),

        // Hats
        OutfitItemDef("hat_none", "Không đội mũ", OutfitCategory.HAT, 0, true, 0, "none"),
        OutfitItemDef("hat_straw", "Mũ rơm nông trại", OutfitCategory.HAT, 0, true, 0, "straw"),
        OutfitItemDef("hat_beret", "Mũ nồi họa sĩ", OutfitCategory.HAT, 120, false, 0, "beret"),
        OutfitItemDef("hat_flower_crown", "Vòng hoa đồng nội", OutfitCategory.HAT, 140, false, 0, "flower_crown"),
        OutfitItemDef("hat_beanie", "Mũ len ấm áp", OutfitCategory.HAT, 100, false, 0, "beanie"),

        // Accessories
        OutfitItemDef("acc_none", "Không phụ kiện", OutfitCategory.ACCESSORY, 0, true, 0, "none"),
        OutfitItemDef("acc_glasses", "Kính tròn tri thức", OutfitCategory.ACCESSORY, 0, true, 0, "glasses"),
        OutfitItemDef("acc_scarf", "Khăn choàng cổ pastel", OutfitCategory.ACCESSORY, 110, false, 0, "scarf"),
        OutfitItemDef("acc_ribbon", "Nơ cài tóc nhỏ", OutfitCategory.ACCESSORY, 80, false, 0, "ribbon"),
        OutfitItemDef("acc_bag", "Túi đeo chéo dã ngoại", OutfitCategory.ACCESSORY, 130, false, 0, "bag")
    )

    data class OutfitPreset(
        val code: String,
        val nameVi: String,
        val topId: String,
        val topColor: Int,
        val bottomId: String,
        val bottomColor: Int,
        val shoesId: String,
        val hatId: String,
        val accessoryId: String
    )

    val playerPresets = listOf(
        OutfitPreset("P01", "Sơ mi trắng & Jeans", "top_shirt_white", 0, "bot_jeans", 1, "shoe_sneaker", "hat_none", "acc_none"),
        OutfitPreset("P02", "Áo len Pastel", "top_sweater_pastel", 2, "bot_skirt_casual", 4, "shoe_sneaker", "hat_none", "acc_glasses"),
        OutfitPreset("P03", "Hoodie dễ thương", "top_hoodie_cute", 5, "bot_jeans", 1, "shoe_sneaker", "hat_none", "acc_none"),
        OutfitPreset("P04", "Quần yếm nông dân", "top_tee_sunny", 0, "bot_overalls", 6, "shoe_farm", "hat_straw", "acc_none"),
        OutfitPreset("P05", "Váy hoa mùa xuân", "top_shirt_white", 0, "bot_skirt_floral", 3, "shoe_farm", "hat_none", "acc_ribbon"),
        OutfitPreset("P06", "Trang phục dã ngoại", "top_picnic_vest", 6, "bot_shorts", 5, "shoe_sneaker", "hat_straw", "acc_bag"),
        OutfitPreset("P07", "Đồ ngủ sao trời", "top_pajama", 7, "bot_pajama_pants", 7, "shoe_sneaker", "hat_none", "acc_none"),
        OutfitPreset("P08", "Trang phục hẹn hò", "top_sweater_pastel", 3, "bot_skirt_floral", 8, "shoe_sneaker", "hat_none", "acc_ribbon")
    )

    val crushPresets = listOf(
        OutfitPreset("C01", "Sơ mi trắng thanh lịch", "top_shirt_white", 0, "bot_jeans", 1, "shoe_sneaker", "hat_none", "acc_glasses"),
        OutfitPreset("C02", "Áo len Oversized", "top_sweater_pastel", 7, "bot_jeans", 0, "shoe_sneaker", "hat_none", "acc_none"),
        OutfitPreset("C03", "Trang phục làm vườn", "top_tee_sunny", 4, "bot_overalls", 6, "shoe_farm", "hat_straw", "acc_none"),
        OutfitPreset("C04", "Áo Hoodie ấm áp", "top_hoodie_cute", 2, "bot_shorts", 1, "shoe_sneaker", "hat_none", "acc_none"),
        OutfitPreset("C05", "Trang phục đi dạo", "top_picnic_vest", 5, "bot_skirt_casual", 0, "shoe_sneaker", "hat_none", "acc_bag"),
        OutfitPreset("C06", "Trang phục hẹn hò", "top_sweater_pastel", 3, "bot_skirt_floral", 3, "shoe_sneaker", "hat_none", "acc_ribbon"),
        OutfitPreset("C07", "Trang phục lễ hội", "top_tee_sunny", 8, "bot_skirt_floral", 8, "shoe_farm", "hat_none", "acc_ribbon"),
        OutfitPreset("C08", "Trang phục mùa đông", "top_hoodie_cute", 9, "bot_jeans", 1, "shoe_farm", "hat_none", "acc_none")
    )

    fun defaultPlayerAppearance(gender: Gender): CharacterAppearance =
        if (gender == Gender.MALE) {
            CharacterAppearance(
                gender = Gender.MALE,
                skinToneIndex = 1,
                eyeStyle = EyeStyle.GENTLE,
                eyeColorIndex = 0,
                eyebrowStyle = EyebrowStyle.NATURAL,
                hairStyleId = "m_layer",
                hairColorIndex = 1,
                topId = "top_tee_sunny",
                topColorIndex = 0,
                bottomId = "bot_overalls",
                bottomColorIndex = 6,
                shoesId = "shoe_farm",
                hatId = "hat_straw",
                accessoryId = "acc_none"
            )
        } else {
            CharacterAppearance(
                gender = Gender.FEMALE,
                skinToneIndex = 1,
                eyeStyle = EyeStyle.SPARKLE,
                eyeColorIndex = 1,
                eyebrowStyle = EyebrowStyle.SOFT,
                hairStyleId = "f_layer",
                hairColorIndex = 1,
                topId = "top_shirt_white",
                topColorIndex = 0,
                bottomId = "bot_skirt_floral",
                bottomColorIndex = 3,
                shoesId = "shoe_farm",
                hatId = "hat_none",
                accessoryId = "acc_none"
            )
        }

    fun defaultCrushAppearance(gender: Gender): CharacterAppearance =
        if (gender == Gender.MALE) {
            CharacterAppearance(
                gender = Gender.MALE,
                skinToneIndex = 1,
                eyeStyle = EyeStyle.GENTLE,
                eyeColorIndex = 2,
                eyebrowStyle = EyebrowStyle.SOFT,
                hairStyleId = "m_k_layer",
                hairColorIndex = 0,
                topId = "top_sweater_pastel",
                topColorIndex = 7,
                bottomId = "bot_jeans",
                bottomColorIndex = 1,
                shoesId = "shoe_sneaker",
                hatId = "hat_none",
                accessoryId = "acc_glasses"
            )
        } else {
            CharacterAppearance(
                gender = Gender.FEMALE,
                skinToneIndex = 0,
                eyeStyle = EyeStyle.SMILE,
                eyeColorIndex = 3,
                eyebrowStyle = EyebrowStyle.SOFT,
                hairStyleId = "f_ponytail",
                hairColorIndex = 2,
                topId = "top_sweater_pastel",
                topColorIndex = 3,
                bottomId = "bot_skirt_casual",
                bottomColorIndex = 0,
                shoesId = "shoe_sneaker",
                hatId = "hat_none",
                accessoryId = "acc_none"
            )
        }
}

enum class CropType(
    val id: String,
    val nameVi: String,
    val daysToGrow: Int,
    val seedPrice: Int,
    val sellPrice: Int,
    val energyRestore: Int,
    val crushLikedLevel: Int, // 2 = loved, 1 = liked
    val descriptionVi: String
) {
    TURNIP("turnip", "Củ cải trắng", 2, 15, 38, 12, 1, "Giòn ngọt, lớn rất nhanh trong đất ẩm."),
    STRAWBERRY("strawberry", "Dâu tây ngọt", 3, 30, 82, 20, 2, "Món khoái khẩu ngọt ngào của người thương!"),
    TOMATO("tomato", "Cà chua bi", 3, 25, 65, 16, 1, "Căng mọng nắng sớm, giàu năng lượng."),
    CARROT("carrot", "Cà rốt vàng", 2, 20, 48, 15, 1, "Thơm mùi đất mới, dễ trồng quanh năm."),
    SUNFLOWER("sunflower", "Hướng dương", 4, 40, 115, 10, 2, "Loài hoa mang ánh mặt trời rực rỡ."),
    LAVENDER("lavender", "Oải hương tím", 4, 45, 130, 10, 2, "Hương thơm dịu êm giúp thư giãn tâm hồn."),
    PUMPKIN("pumpkin", "Bí ngô mật", 5, 55, 165, 28, 1, "Quả lớn vàng ươm, giá trị cao."),
    WATERMELON("watermelon", "Dưa hấu mát", 5, 60, 185, 35, 2, "Ngọt mát tuyệt vời cho buổi hẹn bên hồ.")
}

enum class ToolType(val id: String, val nameVi: String) {
    HOE("hoe", "Cuốc đất"),
    WATERING_CAN("watering_can", "Bình tưới"),
    HAND("hand", "Thu hoạch / Tương tác"),
    GIFT("gift", "Tặng quà")
}

enum class ItemCategory {
    TOOL, SEED, CROP, SNACK, GIFT_ITEM
}

data class InventoryItem(
    val id: String,
    val nameVi: String,
    val category: ItemCategory,
    val quantity: Int,
    val cropType: CropType? = null,
    val toolType: ToolType? = null,
    val sellPrice: Int = 0,
    val energyRestore: Int = 0,
    val affectionPoints: Int = 5,
    val descriptionVi: String = ""
)

enum class TileType(val walkable: Boolean) {
    GRASS_LIGHT(true),
    GRASS_MED(true),
    GRASS_FLOWER_PATCH(true),
    DIRT_PATH(true),
    FARM_SOIL(true),
    TILLED_SOIL(true),
    WATERED_SOIL(true),
    WATER(false),
    BRIDGE_WOOD(true),
    GARDEN_STONE(true),
    SHRINE_TILE(true),
    SUNSET_DECK(true)
}

enum class PropType(val solid: Boolean, val widthTiles: Int = 1, val heightTiles: Int = 1) {
    TREE_SMALL(true, 1, 1),
    TREE_OAK(true, 2, 2),
    TREE_PINE(true, 2, 2),
    TREE_SAKURA(true, 2, 2),
    FLOWER_DAISY(false),
    FLOWER_TULIP(false),
    FLOWER_LAVENDER(false),
    FLOWER_SUN(false),
    PLAYER_HOUSE(true, 4, 3),
    CRUSH_HOUSE(true, 4, 3),
    FENCE_H(true),
    FENCE_V(true),
    SHIPPING_BIN(true),
    WELL(true),
    BENCH(true),
    LANTERN(true),
    BUSH(true),
    SCARECROW(true),
    WISHING_CRYSTAL(true),
    MAILBOX(true),
    MIRROR_POST(true) // In-game wardrobe/salon access!
}

data class WorldProp(
    val id: String,
    val type: PropType,
    val tileX: Int,
    val tileY: Int,
    val labelVi: String = ""
)

enum class WorldZone(val id: String, val nameVi: String) {
    FARM("farm", "Nông trại"),
    HOUSE("house", "Ngôi nhà chính"),
    LAKE("lake", "Hồ nước"),
    BRIDGE("bridge", "Cầu gỗ"),
    MEADOW("meadow", "Đồng hoa"),
    GARDEN("garden", "Khu vườn"),
    FOREST("forest", "Rừng cây"),
    SUNSET_HILL("sunset_hill", "Đồi ngắm hoàng hôn"),
    CRUSH_HOUSE("crush_house", "Nhà của Crush"),
    SECRET_GLADE("secret_glade", "Khu vực bí mật")
}

enum class WeatherType(val labelVi: String) {
    SUNNY("Nắng ấm"),
    CLOUDY("Nhiều mây"),
    RAINY("Mưa dịu"),
    FOGGY("Sương mù"),
    METEOR_SHOWER("Mưa sao băng"),
    RAINBOW("Cầu vồng sau mưa")
}

enum class TimePhase(val labelVi: String, val startHour: Int) {
    DAWN("Bình minh", 6),
    MORNING("Buổi sáng", 9),
    NOON("Buổi trưa", 12),
    AFTERNOON("Buổi chiều", 15),
    SUNSET("Hoàng hôn", 17),
    EVENING("Buổi tối", 19),
    NIGHT("Ban đêm", 22)
}

enum class RelationshipTier(val labelVi: String, val minAffection: Int) {
    STRANGER("Người lạ", 0),
    ACQUAINTANCE("Người quen", 20),
    FRIEND("Bạn bè", 40),
    CLOSE_FRIEND("Thân thiết", 60),
    CRUSH("Có cảm tình", 80),
    LOVE("Yêu nhau", 100);

    companion object {
        fun fromAffection(affection: Int): RelationshipTier = when {
            affection >= 100 -> LOVE
            affection >= 80 -> CRUSH
            affection >= 60 -> CLOSE_FRIEND
            affection >= 40 -> FRIEND
            affection >= 20 -> ACQUAINTANCE
            else -> STRANGER
        }
    }
}

data class FarmPlotState(
    val tileX: Int,
    val tileY: Int,
    val tilled: Boolean = false,
    val watered: Boolean = false,
    val cropType: CropType? = null,
    val daysGrown: Int = 0
) {
    val isMature: Boolean
        get() = cropType != null && daysGrown >= cropType.daysToGrow

    val growthStage: Int
        get() {
            val crop = cropType ?: return 0
            if (daysGrown <= 0) return 0
            if (daysGrown >= crop.daysToGrow) return 3
            return ((daysGrown.toFloat() / crop.daysToGrow) * 2f).toInt().coerceIn(1, 2)
        }
}

data class QuestItem(
    val id: String,
    val titleVi: String,
    val descriptionVi: String,
    val progress: Int,
    val target: Int,
    val rewardCoins: Int,
    val rewardAffection: Int,
    val completed: Boolean = false
)

data class MemoryEntry(
    val id: String,
    val day: Int,
    val titleVi: String,
    val descriptionVi: String,
    val zoneNameVi: String,
    val isLoveDiary: Boolean = false
)

data class DialogueOption(
    val labelVi: String,
    val responseVi: String,
    val affectionDelta: Int,
    val crushExpression: Expression,
    val memoryTitleVi: String? = null
)

data class ActiveDialogue(
    val speakerName: String,
    val textVi: String,
    val expression: Expression,
    val options: List<DialogueOption> = emptyList(),
    val isDateEvent: Boolean = false,
    val canCloseDirectly: Boolean = false
)
