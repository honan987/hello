package com.example.game.world

import com.example.game.model.*

/**
 * 36 x 28 Seamless Cottagecore Tilemap connecting all 10 required zones:
 * - Ngôi nhà chính (Top-Left: 4..9, 4..8)
 * - Nông trại (Left-Center: 4..13, 10..18)
 * - Khu vườn (Center: 15..21, 9..15)
 * - Đồng hoa (Bottom-Left: 3..14, 19..25)
 * - Hồ nước (Right-Center: 22..30, 14..22)
 * - Cầu gỗ (Crosses lake inlet: 19..22, 16..17)
 * - Đồi ngắm hoàng hôn (Bottom-Right: 23..33, 22..26)
 * - Nhà của Crush (Top-Right: 24..31, 4..9)
 * - Rừng cây (Top-Center & East: 14..23, 2..8 & 31..35, 10..20)
 * - Khu vực bí mật (Hidden Moonlit Glade: 15..21, 1..5)
 */
object FarmWorldMap {
    const val WIDTH = 36
    const val HEIGHT = 28

    val tiles: Array<Array<TileType>> = Array(HEIGHT) { Array(WIDTH) { TileType.GRASS_LIGHT } }
    val props: List<WorldProp>
    val initialFarmPlots: List<Pair<Int, Int>>

    init {
        val propList = mutableListOf<WorldProp>()
        val farmCoords = mutableListOf<Pair<Int, Int>>()

        // 1. Base varied grass meadow
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                tiles[y][x] = when {
                    (x + y * 3) % 7 == 0 -> TileType.GRASS_FLOWER_PATCH
                    (x * 2 + y) % 5 == 0 -> TileType.GRASS_MED
                    else -> TileType.GRASS_LIGHT
                }
            }
        }

        // 2. Dirt paths connecting all zones smoothly
        fun pathRect(x1: Int, y1: Int, x2: Int, y2: Int, type: TileType = TileType.DIRT_PATH) {
            for (y in y1..y2) for (x in x1..x2) {
                if (x in 0 until WIDTH && y in 0 until HEIGHT) tiles[y][x] = type
            }
        }
        pathRect(6, 8, 8, 19) // House down to farm & meadow
        pathRect(8, 12, 23, 13) // Main east-west road across garden toward Crush house
        pathRect(23, 8, 25, 13) // Up to Crush House
        pathRect(14, 16, 23, 17) // Path across wooden bridge to lake & sunset hill
        pathRect(22, 22, 29, 23) // Path to Sunset Hill deck
        pathRect(17, 5, 18, 12) // Secret glade path through forest

        // 3. Farm Soil Grid (6x5 fertile plot area)
        for (y in 11..15) {
            for (x in 4..9) {
                if (x != 7 && y != 13) { // keep small walking lane or clean 5x4 blocks
                    tiles[y][x] = TileType.FARM_SOIL
                    farmCoords.add(x to y)
                } else if (x in 4..9 && y in 11..15) {
                    tiles[y][x] = TileType.FARM_SOIL
                    farmCoords.add(x to y)
                }
            }
        }
        // Clear center walking aisle in farm? Actually 5x4 solid soil + border path is even nicer:
        farmCoords.clear()
        for (y in 11..15) {
            for (x in 3..8) {
                tiles[y][x] = TileType.FARM_SOIL
                farmCoords.add(x to y)
            }
        }
        pathRect(9, 10, 10, 17) // Clean path right of farm

        // 4. Crystal Lake (Hồ nước)
        for (y in 15..21) {
            for (x in 21..29) {
                val edge = (x == 21 && (y == 15 || y == 21)) || (x == 29 && (y == 15 || y == 21))
                if (!edge) tiles[y][x] = TileType.WATER
            }
        }
        // Stream inlet under bridge
        for (y in 14..22) {
            if (tiles[y][20] != TileType.DIRT_PATH) tiles[y][20] = TileType.WATER
        }

        // 5. Wooden Bridge (Cầu gỗ) over water inlet at x=19..21, y=16..17
        for (x in 19..21) {
            tiles[16][x] = TileType.BRIDGE_WOOD
            tiles[17][x] = TileType.BRIDGE_WOOD
        }

        // 6. Herb & Blossom Garden (Khu vườn)
        pathRect(14, 9, 19, 11, TileType.GARDEN_STONE)
        pathRect(15, 10, 18, 10, TileType.GRASS_FLOWER_PATCH)

        // 7. Sunset Hill Overlook (Đồi ngắm hoàng hôn)
        pathRect(25, 23, 30, 25, TileType.SUNSET_DECK)

        // 8. Secret Glade (Khu vực bí mật)
        pathRect(15, 2, 20, 5, TileType.SHRINE_TILE)

        // 9. Buildings & Landmarks
        propList += WorldProp("player_house", PropType.PLAYER_HOUSE, 4, 4, "Ngôi nhà chính (Nghỉ ngơi)")
        propList += WorldProp("crush_house", PropType.CRUSH_HOUSE, 24, 4, "Nhà của Crush")
        propList += WorldProp("shipping_bin", PropType.SHIPPING_BIN, 10, 9, "Thùng bán nông sản")
        propList += WorldProp("mirror_post", PropType.MIRROR_POST, 3, 8, "Gương thay trang phục")
        propList += WorldProp("farm_well", PropType.WELL, 11, 14, "Giếng nước mát")
        propList += WorldProp("scarecrow", PropType.SCARECROW, 2, 13, "Bù nhìn rơm")
        propList += WorldProp("mailbox", PropType.MAILBOX, 23, 8, "Hòm thư của Crush")
        propList += WorldProp("sunset_bench", PropType.BENCH, 27, 23, "Ghế ngắm hoàng hôn")
        propList += WorldProp("garden_bench", PropType.BENCH, 15, 9, "Ghế đọc sách")
        propList += WorldProp("wishing_crystal", PropType.WISHING_CRYSTAL, 17, 2, "Đá ước nguyện bí mật")

        // Lanterns along paths & lake
        for ((lx, ly) in listOf(9 to 8, 13 to 12, 19 to 15, 24 to 22, 23 to 9, 16 to 4)) {
            propList += WorldProp("lantern_${lx}_$ly", PropType.LANTERN, lx, ly, "Đèn lồng")
        }

        // Fences around farm
        for (fx in 3..8) {
            propList += WorldProp("fence_t_$fx", PropType.FENCE_H, fx, 10)
        }

        // Trees (Multi-size: Oak, Pine, Sakura, Small) framing Woods, Houses, & Secret Glade
        val trees = listOf(
            PropType.TREE_OAK to (1 to 2), PropType.TREE_PINE to (12 to 2), PropType.TREE_SAKURA to (29 to 2),
            PropType.TREE_OAK to (32 to 5), PropType.TREE_PINE to (13 to 4), PropType.TREE_PINE to (21 to 4),
            PropType.TREE_SAKURA to (14 to 1), PropType.TREE_SAKURA to (20 to 1),
            PropType.TREE_OAK to (1 to 17), PropType.TREE_SAKURA to (21 to 9),
            PropType.TREE_PINE to (31 to 12), PropType.TREE_OAK to (32 to 17),
            PropType.TREE_SAKURA to (31 to 22), PropType.TREE_SMALL to (11 to 6),
            PropType.TREE_SMALL to (22 to 6), PropType.TREE_SMALL to (14 to 22),
            PropType.TREE_SMALL to (18 to 23), PropType.TREE_OAK to (2 to 23)
        )
        trees.forEachIndexed { idx, (type, pos) ->
            propList += WorldProp("tree_$idx", type, pos.first, pos.second)
        }

        // Wildflower Meadow (Đồng hoa) & Garden flowers
        val flowerTypes = listOf(PropType.FLOWER_DAISY, PropType.FLOWER_TULIP, PropType.FLOWER_LAVENDER, PropType.FLOWER_SUN)
        for (fy in 19..25) {
            for (fx in 4..13) {
                if ((fx * 3 + fy * 5) % 4 == 0) {
                    propList += WorldProp(
                        "fl_${fx}_$fy",
                        flowerTypes[(fx + fy) % flowerTypes.size],
                        fx,
                        fy
                    )
                }
            }
        }
        // Flowers around Crush's yard
        for ((fx, fy) in listOf(22 to 7, 23 to 6, 28 to 8, 29 to 7, 15 to 11, 18 to 11)) {
            propList += WorldProp("cfl_${fx}_$fy", flowerTypes[(fx + fy) % flowerTypes.size], fx, fy)
        }

        props = propList
        initialFarmPlots = farmCoords
    }

    fun isWalkable(tileX: Int, tileY: Int): Boolean {
        if (tileX !in 1 until WIDTH - 1 || tileY !in 1 until HEIGHT - 1) return false
        if (!tiles[tileY][tileX].walkable) return false
        for (p in props) {
            if (!p.type.solid) continue
            // Buildings have collision on their lower footprint so player can walk behind roof top!
            val colStartY = if (p.type == PropType.PLAYER_HOUSE || p.type == PropType.CRUSH_HOUSE) p.tileY + 1 else p.tileY
            if (tileX in p.tileX until (p.tileX + p.type.widthTiles) &&
                tileY in colStartY until (p.tileY + p.type.heightTiles)
            ) {
                return false
            }
        }
        return true
    }

    fun getZoneAt(tileX: Int, tileY: Int): WorldZone = when {
        tileX in 14..21 && tileY in 1..6 -> WorldZone.SECRET_GLADE
        tileX in 23..31 && tileY in 3..9 -> WorldZone.CRUSH_HOUSE
        tileX in 2..10 && tileY in 3..9 -> WorldZone.HOUSE
        tileX in 2..11 && tileY in 10..17 -> WorldZone.FARM
        tileX in 18..22 && tileY in 15..18 -> WorldZone.BRIDGE
        tileX in 20..30 && tileY in 14..21 -> WorldZone.LAKE
        tileX in 24..33 && tileY in 22..26 -> WorldZone.SUNSET_HILL
        tileX in 13..20 && tileY in 8..14 -> WorldZone.GARDEN
        tileX in 3..15 && tileY in 18..26 -> WorldZone.MEADOW
        else -> WorldZone.FOREST
    }
}
