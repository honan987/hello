package com.example.game.pixel

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.model.*

/**
 * Generates crisp pixel-art tiles, crops, props, buildings, and item icons.
 * Every asset is drawn pixel-by-pixel with shading, outlines, and highlights.
 */
object WorldTileAtlas {

    private val cache = LruCache<String, ImageBitmap>(256)

    private class Px(val w: Int, val h: Int) {
        val data = IntArray(w * h) { Color.TRANSPARENT }
        fun set(x: Int, y: Int, c: Int) {
            if (x in 0 until w && y in 0 until h) data[y * w + x] = c
        }
        fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
            for (dy in 0 until rh) for (dx in 0 until rw) set(x + dx, y + dy, c)
        }
        fun circle(cx: Int, cy: Int, r: Int, c: Int) {
            for (y in cy - r..cy + r) {
                for (x in cx - r..cx + r) {
                    if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) set(x, y, c)
                }
            }
        }
        fun fill(c: Int) = data.fill(c)
        fun toImage(): ImageBitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.setPixels(data, 0, w, 0, 0, w, h)
            return bmp.asImageBitmap()
        }
    }

    fun getTile(type: TileType, animFrame: Int, variant: Int = 0): ImageBitmap {
        val wf = if (type == TileType.WATER) animFrame % 3 else 0
        val v = variant % 4
        val key = "tile_${type.name}_${wf}_$v"
        cache.get(key)?.let { return it }
        val p = Px(16, 16)
        when (type) {
            TileType.GRASS_LIGHT -> {
                p.fill(PixelPalette.GRASS_LIGHT_A)
                p.set(2 + v * 2, 3, PixelPalette.GRASS_LIGHT_B)
                p.set(3 + v * 2, 2, Color.rgb(174, 226, 144))
                p.set(11 - v, 11, PixelPalette.GRASS_LIGHT_B)
                p.set(12 - v, 10, Color.rgb(174, 226, 144))
                p.set(6, 13, PixelPalette.GRASS_MED_A)
            }
            TileType.GRASS_MED -> {
                p.fill(PixelPalette.GRASS_MED_A)
                p.set(4, 4 + v, PixelPalette.GRASS_MED_B)
                p.set(5, 3 + v, PixelPalette.GRASS_LIGHT_A)
                p.set(12, 9, PixelPalette.GRASS_MED_B)
                p.set(13, 8, PixelPalette.GRASS_LIGHT_A)
            }
            TileType.GRASS_FLOWER_PATCH -> {
                p.fill(PixelPalette.GRASS_LIGHT_A)
                p.set(3, 4, Color.rgb(255, 240, 148))
                p.set(4, 4, Color.rgb(255, 250, 240))
                p.set(11, 11, Color.rgb(245, 162, 192))
                p.set(12, 11, Color.rgb(255, 232, 132))
                p.set(7, 8, PixelPalette.GRASS_MED_A)
            }
            TileType.DIRT_PATH -> {
                p.fill(PixelPalette.DIRT_A)
                p.rect(2 + v, 3, 2, 1, PixelPalette.DIRT_B)
                p.rect(9, 10 - v, 3, 1, PixelPalette.DIRT_B)
                p.set(5, 13, Color.rgb(228, 192, 152))
                p.set(13, 4, Color.rgb(184, 142, 102))
            }
            TileType.FARM_SOIL -> {
                p.fill(PixelPalette.SOIL_DRY)
                p.rect(0, 0, 16, 1, PixelPalette.DIRT_B)
                p.rect(0, 15, 16, 1, Color.rgb(138, 92, 58))
                p.set(4, 5, Color.rgb(142, 96, 62))
                p.set(11, 10, Color.rgb(184, 134, 92))
            }
            TileType.TILLED_SOIL -> {
                p.fill(PixelPalette.SOIL_DRY)
                for (row in listOf(3, 7, 11)) {
                    p.rect(2, row, 12, 2, PixelPalette.SOIL_TILLED)
                    p.rect(2, row - 1, 12, 1, Color.rgb(184, 134, 92))
                }
            }
            TileType.WATERED_SOIL -> {
                p.fill(Color.rgb(108, 70, 48))
                for (row in listOf(3, 7, 11)) {
                    p.rect(2, row, 12, 2, PixelPalette.SOIL_WATERED)
                    p.rect(2, row - 1, 12, 1, Color.rgb(124, 82, 56))
                }
                p.set(5, 4, Color.rgb(126, 186, 218))
                p.set(10, 8, Color.rgb(126, 186, 218))
            }
            TileType.WATER -> {
                p.fill(PixelPalette.WATER_A)
                val shift = wf * 3
                p.rect((2 + shift) % 13, 3, 4, 1, PixelPalette.WATER_B)
                p.rect((8 + shift) % 12, 9, 4, 1, PixelPalette.WATER_FOAM)
                p.rect((4 + shift) % 12, 13, 3, 1, PixelPalette.WATER_B)
                if (v == 1 && wf == 1) {
                    p.set(6, 6, Color.WHITE)
                }
            }
            TileType.BRIDGE_WOOD -> {
                p.fill(PixelPalette.WOOD_LIGHT)
                for (x in 0 until 16 step 4) {
                    p.rect(x, 0, 1, 16, PixelPalette.WOOD_DARK)
                }
                p.rect(0, 0, 16, 2, PixelPalette.WOOD_DARK)
                p.rect(0, 14, 16, 2, PixelPalette.WOOD_DARK)
                p.rect(1, 1, 14, 1, Color.rgb(212, 160, 112))
            }
            TileType.GARDEN_STONE -> {
                p.fill(PixelPalette.GRASS_LIGHT_A)
                p.rect(2, 2, 5, 5, Color.rgb(214, 208, 198))
                p.rect(9, 3, 5, 4, Color.rgb(198, 192, 182))
                p.rect(3, 9, 6, 5, Color.rgb(204, 198, 188))
                p.rect(10, 9, 4, 5, Color.rgb(220, 214, 204))
                p.rect(2, 6, 5, 1, Color.rgb(156, 148, 140))
            }
            TileType.SHRINE_TILE -> {
                p.fill(Color.rgb(184, 198, 174))
                p.rect(1, 1, 14, 14, Color.rgb(206, 218, 198))
                p.rect(3, 3, 10, 10, Color.rgb(168, 214, 196))
                p.set(7, 7, Color.rgb(232, 250, 255))
                p.set(8, 8, Color.rgb(232, 250, 255))
            }
            TileType.SUNSET_DECK -> {
                p.fill(Color.rgb(198, 142, 96))
                for (y in 0 until 16 step 4) {
                    p.rect(0, y, 16, 1, Color.rgb(148, 98, 62))
                }
                p.set(3, 2, Color.rgb(224, 174, 126))
                p.set(11, 10, Color.rgb(224, 174, 126))
            }
        }
        return p.toImage().also { cache.put(key, it) }
    }

    fun getCropSprite(crop: CropType, stage: Int): ImageBitmap {
        val key = "crop_${crop.id}_$stage"
        cache.get(key)?.let { return it }
        val p = Px(16, 20)
        val leafLight = Color.rgb(146, 216, 112)
        val leafBase = Color.rgb(96, 172, 76)
        val leafDark = Color.rgb(58, 118, 48)

        when (stage) {
            0 -> {
                // Planted seeds
                val seedCol = Color.rgb(232, 204, 134)
                p.rect(5, 14, 2, 2, seedCol)
                p.rect(9, 15, 2, 2, seedCol)
                p.rect(7, 12, 2, 2, seedCol)
            }
            1 -> {
                // Tiny twin sprout
                p.rect(7, 13, 2, 4, leafDark)
                p.rect(5, 11, 3, 2, leafLight)
                p.rect(8, 11, 3, 2, leafBase)
            }
            2 -> {
                // Leafy mid-stage
                p.rect(6, 10, 4, 7, leafDark)
                p.rect(4, 8, 8, 5, leafBase)
                p.rect(5, 7, 6, 3, leafLight)
            }
            else -> {
                // Mature lush plant + distinct fruit/flower!
                p.rect(4, 9, 8, 8, leafDark)
                p.rect(3, 7, 10, 7, leafBase)
                p.rect(5, 6, 6, 4, leafLight)
                when (crop) {
                    CropType.TURNIP -> {
                        p.rect(5, 11, 6, 6, Color.rgb(248, 244, 238))
                        p.rect(6, 15, 4, 2, Color.rgb(218, 208, 224))
                        p.rect(5, 4, 6, 4, leafLight)
                    }
                    CropType.STRAWBERRY -> {
                        for ((bx, by) in listOf(4 to 10, 9 to 11, 6 to 13)) {
                            p.rect(bx, by, 3, 3, Color.rgb(238, 76, 98))
                            p.set(bx + 1, by + 1, Color.rgb(255, 224, 152))
                        }
                    }
                    CropType.TOMATO -> {
                        p.rect(4, 9, 3, 3, Color.rgb(238, 82, 68))
                        p.rect(9, 11, 3, 3, Color.rgb(238, 82, 68))
                        p.set(5, 9, Color.rgb(255, 174, 158))
                    }
                    CropType.CARROT -> {
                        p.rect(6, 11, 4, 6, Color.rgb(246, 146, 56))
                        p.rect(7, 15, 2, 3, Color.rgb(214, 108, 34))
                        p.rect(4, 4, 8, 5, leafLight)
                    }
                    CropType.SUNFLOWER -> {
                        p.rect(7, 9, 2, 8, leafDark)
                        p.circle(8, 6, 4, Color.rgb(252, 208, 64))
                        p.circle(8, 6, 2, Color.rgb(112, 66, 38))
                    }
                    CropType.LAVENDER -> {
                        for (sx in listOf(4, 7, 10)) {
                            p.rect(sx, 8, 2, 8, leafBase)
                            p.rect(sx, 4, 2, 5, Color.rgb(174, 134, 226))
                            p.set(sx, 5, Color.rgb(218, 192, 248))
                        }
                    }
                    CropType.PUMPKIN -> {
                        p.rect(3, 10, 10, 7, Color.rgb(238, 138, 48))
                        p.rect(5, 10, 6, 7, Color.rgb(250, 166, 72))
                        p.rect(7, 8, 2, 2, leafDark)
                    }
                    CropType.WATERMELON -> {
                        p.rect(3, 10, 10, 7, Color.rgb(58, 142, 72))
                        p.rect(5, 11, 2, 5, Color.rgb(132, 206, 122))
                        p.rect(9, 11, 2, 5, Color.rgb(132, 206, 122))
                    }
                }
            }
        }
        return p.toImage().also { cache.put(key, it) }
    }

    fun getPropSprite(type: PropType, animFrame: Int = 0): ImageBitmap {
        val f = if (type == PropType.WISHING_CRYSTAL || type == PropType.LANTERN) animFrame % 2 else 0
        val key = "prop_${type.name}_$f"
        cache.get(key)?.let { return it }

        val p = when (type) {
            PropType.TREE_SMALL -> Px(16, 24).apply {
                rect(6, 15, 4, 8, Color.rgb(122, 78, 48))
                circle(8, 10, 6, Color.rgb(88, 158, 82))
                circle(7, 8, 4, Color.rgb(136, 204, 116))
            }
            PropType.TREE_OAK -> Px(32, 42).apply {
                rect(13, 24, 6, 16, Color.rgb(118, 74, 44))
                rect(15, 25, 2, 14, Color.rgb(148, 98, 62))
                circle(16, 16, 12, Color.rgb(72, 142, 72))
                circle(11, 18, 7, Color.rgb(92, 166, 86))
                circle(21, 18, 7, Color.rgb(82, 154, 78))
                circle(15, 12, 8, Color.rgb(136, 202, 114))
                set(12, 10, Color.rgb(174, 228, 146))
                set(18, 12, Color.rgb(174, 228, 146))
            }
            PropType.TREE_PINE -> Px(32, 44).apply {
                rect(13, 30, 6, 12, Color.rgb(104, 66, 42))
                for (row in 0..10) {
                    val hw = 2 + row
                    rect(16 - hw / 2, 4 + row, hw, 1, Color.rgb(58, 124, 82))
                }
                for (row in 0..10) {
                    val hw = 3 + row
                    rect(16 - hw / 2, 13 + row, hw, 1, Color.rgb(72, 142, 92))
                }
                for (row in 0..10) {
                    val hw = 4 + row
                    rect(16 - hw / 2, 22 + row, hw, 1, Color.rgb(52, 112, 74))
                }
            }
            PropType.TREE_SAKURA -> Px(32, 42).apply {
                rect(13, 24, 6, 16, Color.rgb(112, 72, 64))
                circle(16, 16, 12, Color.rgb(232, 138, 174))
                circle(11, 18, 7, Color.rgb(246, 172, 202))
                circle(21, 18, 7, Color.rgb(246, 172, 202))
                circle(15, 12, 8, Color.rgb(255, 208, 228))
                set(12, 10, Color.WHITE)
                set(19, 14, Color.WHITE)
            }
            PropType.FLOWER_DAISY -> Px(16, 16).apply {
                rect(7, 9, 2, 5, Color.rgb(88, 164, 78))
                rect(6, 5, 4, 4, Color.WHITE)
                rect(7, 6, 2, 2, Color.rgb(252, 212, 74))
            }
            PropType.FLOWER_TULIP -> Px(16, 16).apply {
                rect(7, 9, 2, 5, Color.rgb(88, 164, 78))
                rect(5, 5, 6, 4, Color.rgb(244, 122, 152))
                set(6, 4, Color.rgb(255, 166, 192))
                set(9, 4, Color.rgb(255, 166, 192))
            }
            PropType.FLOWER_LAVENDER -> Px(16, 16).apply {
                rect(7, 9, 2, 5, Color.rgb(88, 164, 78))
                rect(6, 3, 4, 6, Color.rgb(174, 132, 226))
                set(7, 4, Color.rgb(218, 192, 248))
            }
            PropType.FLOWER_SUN -> Px(16, 16).apply {
                rect(7, 9, 2, 5, Color.rgb(88, 164, 78))
                circle(8, 6, 3, Color.rgb(252, 206, 62))
                rect(7, 5, 2, 2, Color.rgb(112, 66, 38))
            }
            PropType.PLAYER_HOUSE -> drawCottage(
                roofBase = Color.rgb(212, 114, 96),
                roofLight = Color.rgb(236, 148, 128),
                wallBase = Color.rgb(252, 238, 212),
                trim = Color.rgb(138, 88, 54)
            )
            PropType.CRUSH_HOUSE -> drawCottage(
                roofBase = Color.rgb(154, 122, 188),
                roofLight = Color.rgb(186, 158, 222),
                wallBase = Color.rgb(255, 242, 246),
                trim = Color.rgb(156, 102, 118)
            )
            PropType.FENCE_H -> Px(16, 16).apply {
                rect(1, 5, 3, 10, Color.rgb(162, 108, 68))
                rect(12, 5, 3, 10, Color.rgb(162, 108, 68))
                rect(0, 7, 16, 2, Color.rgb(192, 138, 92))
                rect(0, 11, 16, 2, Color.rgb(174, 118, 76))
            }
            PropType.FENCE_V -> Px(16, 16).apply {
                rect(6, 0, 4, 16, Color.rgb(162, 108, 68))
                rect(7, 1, 2, 14, Color.rgb(192, 138, 92))
            }
            PropType.SHIPPING_BIN -> Px(16, 16).apply {
                rect(2, 4, 12, 11, Color.rgb(148, 96, 56))
                rect(1, 3, 14, 3, Color.rgb(186, 128, 78))
                rect(6, 7, 4, 2, Color.rgb(245, 206, 98))
            }
            PropType.WELL -> Px(16, 20).apply {
                rect(2, 10, 12, 8, Color.rgb(176, 172, 166))
                rect(3, 10, 10, 2, PixelPalette.WATER_A)
                rect(2, 3, 2, 8, Color.rgb(138, 92, 56))
                rect(12, 3, 2, 8, Color.rgb(138, 92, 56))
                rect(1, 1, 14, 3, Color.rgb(192, 108, 92))
            }
            PropType.BENCH -> Px(24, 16).apply {
                rect(2, 5, 20, 4, Color.rgb(184, 126, 78))
                rect(2, 9, 20, 3, Color.rgb(154, 102, 58))
                rect(4, 12, 2, 3, Color.rgb(104, 66, 38))
                rect(18, 12, 2, 3, Color.rgb(104, 66, 38))
            }
            PropType.LANTERN -> Px(16, 22).apply {
                rect(7, 7, 2, 14, Color.rgb(78, 66, 62))
                rect(5, 3, 6, 5, Color.rgb(88, 74, 70))
                val glow = if (f == 0) Color.rgb(255, 228, 132) else Color.rgb(255, 244, 182)
                rect(6, 4, 4, 3, glow)
            }
            PropType.BUSH -> Px(16, 16).apply {
                circle(8, 9, 6, Color.rgb(84, 154, 78))
                circle(7, 7, 4, Color.rgb(122, 192, 106))
                set(5, 8, Color.rgb(244, 122, 148))
                set(10, 7, Color.rgb(244, 122, 148))
            }
            PropType.SCARECROW -> Px(16, 22).apply {
                rect(7, 9, 2, 12, Color.rgb(138, 92, 54))
                rect(2, 11, 12, 2, Color.rgb(138, 92, 54))
                rect(5, 10, 6, 6, Color.rgb(122, 166, 204))
                rect(5, 4, 6, 5, Color.rgb(236, 198, 134))
                rect(3, 4, 10, 2, Color.rgb(214, 162, 82))
                rect(5, 2, 6, 2, Color.rgb(214, 162, 82))
            }
            PropType.WISHING_CRYSTAL -> Px(16, 22).apply {
                rect(4, 16, 8, 5, Color.rgb(154, 162, 174))
                val c1 = if (f == 0) Color.rgb(142, 228, 246) else Color.rgb(186, 244, 255)
                val c2 = Color.rgb(198, 162, 246)
                rect(6, 5, 4, 11, c1)
                rect(7, 3, 2, 12, Color.WHITE)
                rect(5, 8, 2, 6, c2)
            }
            PropType.MAILBOX -> Px(16, 18).apply {
                rect(7, 9, 2, 8, Color.rgb(138, 92, 54))
                rect(4, 5, 8, 5, Color.rgb(236, 148, 166))
                rect(10, 4, 2, 3, Color.rgb(252, 214, 112))
            }
            PropType.MIRROR_POST -> Px(16, 22).apply {
                rect(4, 3, 8, 14, Color.rgb(152, 102, 62))
                rect(5, 4, 6, 12, Color.rgb(192, 234, 248))
                set(6, 6, Color.WHITE)
                set(7, 5, Color.WHITE)
                rect(7, 17, 2, 4, Color.rgb(112, 72, 42))
            }
        }
        return p.toImage().also { cache.put(key, it) }
    }

    private fun drawCottage(roofBase: Int, roofLight: Int, wallBase: Int, trim: Int): Px {
        return Px(64, 52).apply {
            // Chimney
            rect(46, 2, 7, 16, Color.rgb(168, 112, 98))
            rect(45, 2, 9, 2, Color.rgb(132, 84, 72))
            // Pitched Roof
            for (y in 0 until 20) {
                val inset = (20 - y) / 2
                rect(4 + inset, 6 + y, 56 - inset * 2, 1, if (y % 4 == 0) roofLight else roofBase)
            }
            // Roof trim
            rect(4, 25, 56, 2, trim)
            // Walls
            rect(7, 27, 50, 22, wallBase)
            rect(7, 27, 3, 22, trim)
            rect(54, 27, 3, 22, trim)
            rect(7, 47, 50, 3, Color.rgb(142, 134, 128))
            // Warm Door
            rect(27, 34, 10, 15, trim)
            rect(28, 35, 8, 14, Color.rgb(174, 118, 76))
            rect(34, 41, 2, 2, Color.rgb(252, 214, 96))
            // Glowing Windows
            for (wx in listOf(13, 43)) {
                rect(wx, 32, 8, 8, trim)
                rect(wx + 1, 33, 6, 6, Color.rgb(255, 232, 148))
                rect(wx + 3, 33, 2, 6, trim)
                rect(wx + 1, 35, 6, 2, trim)
                // Flower box
                rect(wx - 1, 40, 10, 3, Color.rgb(142, 92, 54))
                set(wx + 1, 39, Color.rgb(245, 132, 162))
                set(wx + 4, 39, Color.rgb(255, 226, 112))
                set(wx + 7, 39, Color.rgb(245, 132, 162))
            }
        }
    }

    fun getItemIcon(item: InventoryItem): ImageBitmap {
        val key = "item_${item.id}"
        cache.get(key)?.let { return it }
        val p = Px(16, 16)
        when (item.category) {
            ItemCategory.TOOL -> when (item.toolType) {
                ToolType.HOE -> {
                    for (i in 3..12) p.rect(i, 15 - i, 2, 2, Color.rgb(148, 96, 56))
                    p.rect(9, 2, 5, 3, Color.rgb(174, 188, 204))
                }
                ToolType.WATERING_CAN -> {
                    p.rect(5, 6, 7, 6, Color.rgb(112, 204, 182))
                    p.rect(6, 4, 5, 2, Color.rgb(84, 174, 154))
                    p.rect(2, 6, 3, 2, Color.rgb(112, 204, 182))
                    p.rect(12, 7, 2, 4, Color.rgb(84, 174, 154))
                }
                ToolType.HAND -> {
                    p.circle(8, 8, 4, Color.rgb(244, 198, 168))
                    p.rect(6, 3, 4, 4, Color.rgb(244, 198, 168))
                }
                ToolType.GIFT -> {
                    p.rect(3, 5, 10, 8, Color.rgb(244, 146, 172))
                    p.rect(7, 5, 2, 8, Color.rgb(255, 224, 128))
                    p.rect(3, 8, 10, 2, Color.rgb(255, 224, 128))
                }
                null -> {}
            }
            ItemCategory.SEED -> {
                p.rect(4, 3, 8, 10, Color.rgb(232, 202, 154))
                p.rect(5, 2, 6, 2, Color.rgb(198, 162, 114))
                val badge = when (item.cropType) {
                    CropType.STRAWBERRY -> Color.rgb(238, 82, 102)
                    CropType.SUNFLOWER -> Color.rgb(252, 206, 64)
                    CropType.LAVENDER -> Color.rgb(174, 134, 226)
                    CropType.WATERMELON -> Color.rgb(72, 166, 88)
                    else -> Color.rgb(122, 192, 106)
                }
                p.rect(6, 6, 4, 4, badge)
            }
            ItemCategory.CROP -> {
                return item.cropType?.let { getCropSprite(it, 3) } ?: p.toImage()
            }
            ItemCategory.SNACK -> {
                p.circle(8, 9, 4, Color.rgb(238, 164, 96))
                p.rect(6, 5, 4, 2, Color.rgb(252, 212, 142))
            }
            ItemCategory.GIFT_ITEM -> {
                p.rect(3, 5, 10, 8, Color.rgb(245, 142, 174))
                p.rect(7, 4, 2, 9, Color.rgb(255, 232, 132))
            }
        }
        return p.toImage().also { cache.put(key, it) }
    }
}
