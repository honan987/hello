package com.example.game.pixel

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.model.*

/**
 * 8-Layer 2D Pixel Art Character Sprite & Portrait Renderer.
 * Renders 24x32 crisp sprites (4 directions, 15 actions, blink & stride frames)
 * and 32x32 expressive portraits with zero anti-aliasing blur.
 */
object CharacterSpriteRenderer {

    private val spriteCache = LruCache<String, ImageBitmap>(512)
    private val portraitCache = LruCache<String, ImageBitmap>(128)

    fun clearCache() {
        spriteCache.evictAll()
        portraitCache.evictAll()
    }

    fun getSprite(
        appearance: CharacterAppearance,
        direction: Direction,
        action: CharacterAction,
        frame: Int,
        blink: Boolean = false
    ): ImageBitmap {
        val animFrame = frame % 4
        val key = "${appearance.hashCode()}_${direction.name}_${action.name}_${animFrame}_$blink"
        spriteCache.get(key)?.let { return it }
        val bmp = renderSpriteBitmap(appearance, direction, action, animFrame, blink)
        val img = bmp.asImageBitmap()
        spriteCache.put(key, img)
        return img
    }

    fun getPortrait(
        appearance: CharacterAppearance,
        expressionOverride: Expression? = null,
        blink: Boolean = false
    ): ImageBitmap {
        val exp = expressionOverride ?: appearance.expression
        val key = "p_${appearance.copy(expression = exp).hashCode()}_$blink"
        portraitCache.get(key)?.let { return it }
        val bmp = renderPortraitBitmap(appearance.copy(expression = exp), blink)
        val img = bmp.asImageBitmap()
        portraitCache.put(key, img)
        return img
    }

    private class PixelCanvas(val w: Int, val h: Int) {
        val pixels = IntArray(w * h) { Color.TRANSPARENT }

        fun set(x: Int, y: Int, color: Int) {
            if (x in 0 until w && y in 0 until h && color != Color.TRANSPARENT) {
                pixels[y * w + x] = color
            }
        }

        fun get(x: Int, y: Int): Int {
            if (x !in 0 until w || y !in 0 until h) return Color.TRANSPARENT
            return pixels[y * w + x]
        }

        fun rect(x: Int, y: Int, rw: Int, rh: Int, color: Int) {
            for (dy in 0 until rh) {
                for (dx in 0 until rw) {
                    set(x + dx, y + dy, color)
                }
            }
        }

        fun toBitmap(): Bitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, w, 0, 0, w, h)
            return bmp
        }
    }

    private fun renderSpriteBitmap(
        app: CharacterAppearance,
        dir: Direction,
        action: CharacterAction,
        frame: Int,
        blink: Boolean
    ): Bitmap {
        val c = PixelCanvas(24, 32)
        val skin = PixelPalette.skinRamps[app.skinToneIndex.coerceIn(PixelPalette.skinRamps.indices)]
        val hair = PixelPalette.hairRamps[app.hairColorIndex.coerceIn(PixelPalette.hairRamps.indices)]
        val topRamp = PixelPalette.clothRamps[app.topColorIndex.coerceIn(PixelPalette.clothRamps.indices)]
        val botRamp = PixelPalette.clothRamps[app.bottomColorIndex.coerceIn(PixelPalette.clothRamps.indices)]
        val eyeColor = PixelPalette.eyeColors[app.eyeColorIndex.coerceIn(PixelPalette.eyeColors.indices)].second

        // Subtle bob & stride offsets
        val bobY = when (action) {
            CharacterAction.IDLE -> if (frame == 1 || frame == 2) 1 else 0
            CharacterAction.WALK -> if (frame == 1 || frame == 3) -1 else 0
            CharacterAction.RUN -> if (frame == 1 || frame == 3) -2 else 0
            CharacterAction.HAPPY, CharacterAction.LOVE -> if (frame % 2 == 0) -1 else 0
            CharacterAction.SIT -> 3
            CharacterAction.SLEEP -> 4
            CharacterAction.HOE, CharacterAction.WATER, CharacterAction.HARVEST -> if (frame % 2 == 1) 1 else 0
            else -> 0
        }

        val legSwing = when (action) {
            CharacterAction.WALK -> when (frame) { 1 -> 1; 3 -> -1; else -> 0 }
            CharacterAction.RUN -> when (frame) { 1 -> 2; 3 -> -2; else -> 0 }
            else -> 0
        }

        val hy = 4 + bobY // head top Y
        val by = 17 + bobY // body top Y

        // Soft shadow under character
        if (action != CharacterAction.SLEEP) {
            c.rect(7, 29, 10, 2, Color.argb(70, 40, 25, 18))
            c.rect(8, 28, 8, 1, Color.argb(45, 40, 25, 18))
        }

        // LAYER 0: Back-layer long hair (rendered behind body when facing DOWN/LEFT/RIGHT)
        if (dir != Direction.UP) {
            drawHairBackLayer(c, app.hairStyleId, dir, hy, hair)
        }

        // LAYER 1: BODY (Legs, Torso base, Arms, Head base)
        drawBodyAndLimbs(c, app.gender, dir, action, frame, legSwing, hy, by, skin)

        // LAYER 7 (Shoes) + LAYER 6 (Pants/Skirt)
        drawShoes(c, app.shoesId, dir, action, legSwing, by)
        drawBottom(c, app.bottomId, app.gender, dir, action, legSwing, by, botRamp)

        // LAYER 5: SHIRT / TOP
        drawTop(c, app.topId, app.gender, dir, action, frame, by, topRamp, botRamp)

        // Overalls straps over top if overalls selected
        if (app.bottomId == "bot_overalls") {
            drawOverallsStraps(c, dir, by, botRamp)
        }

        // Tool / Gift props in hand
        drawActionProp(c, action, dir, frame, by)

        // LAYER 2 & 3: FACE, EYES & EXPRESSION
        if (dir != Direction.UP) {
            val exp = when (action) {
                CharacterAction.HAPPY -> Expression.HAPPY
                CharacterAction.SHY -> Expression.SHY
                CharacterAction.SAD -> Expression.SAD
                CharacterAction.LOVE, CharacterAction.GIFT -> Expression.LOVE
                CharacterAction.SLEEP -> Expression.NEUTRAL
                else -> app.expression
            }
            drawFaceAndEyes(c, dir, hy, skin, app.eyeStyle, eyeColor, exp, blink || action == CharacterAction.SLEEP, hair.shadow)
        }

        // LAYER 4: HAIR (Front / Side / Top / Back)
        drawHairFrontAndTop(c, app.hairStyleId, dir, hy, hair)

        // LAYER 8: HATS & ACCESSORIES
        drawHat(c, app.hatId, dir, hy)
        drawAccessory(c, app.accessoryId, dir, hy, by)

        // Emotion particles on sprite
        if (action == CharacterAction.LOVE || app.expression == Expression.LOVE) {
            val heartY = (hy - 3 - (frame % 2)).coerceAtLeast(0)
            c.set(18, heartY, Color.rgb(245, 96, 130))
            c.set(20, heartY, Color.rgb(245, 96, 130))
            c.set(19, heartY + 1, Color.rgb(245, 96, 130))
            c.set(18, heartY + 1, Color.rgb(245, 96, 130))
            c.set(20, heartY + 1, Color.rgb(245, 96, 130))
            c.set(19, heartY + 2, Color.rgb(245, 96, 130))
        }

        // Crisp 1px outer silhouette outline for dark contrast
        applySoftOutline(c)
        return c.toBitmap()
    }

    private fun drawBodyAndLimbs(
        c: PixelCanvas,
        gender: Gender,
        dir: Direction,
        action: CharacterAction,
        frame: Int,
        legSwing: Int,
        hy: Int,
        by: Int,
        skin: ColorRamp
    ) {
        // Head (12x12 rounded)
        c.rect(7, hy, 10, 12, skin.base)
        c.rect(6, hy + 2, 12, 9, skin.base)
        c.rect(7, hy + 1, 10, 2, skin.highlight)
        // Neck & chin shadow
        c.rect(8, hy + 11, 8, 1, skin.shadow)
        c.rect(10, by - 1, 4, 2, skin.shadow)

        // Ears
        when (dir) {
            Direction.DOWN, Direction.UP -> {
                c.rect(5, hy + 6, 1, 3, skin.shadow)
                c.rect(18, hy + 6, 1, 3, skin.shadow)
            }
            Direction.LEFT -> c.rect(12, hy + 6, 2, 3, skin.shadow)
            Direction.RIGHT -> c.rect(10, hy + 6, 2, 3, skin.shadow)
        }

        // Torso skin base
        val torsoW = if (gender == Gender.MALE) 8 else 6
        val torsoX = 12 - torsoW / 2
        c.rect(torsoX, by, torsoW, 7, skin.base)

        // Arms
        val armRaise = when (action) {
            CharacterAction.WAVE -> if (frame % 2 == 0) -3 else -2
            CharacterAction.HOE, CharacterAction.HARVEST -> if (frame % 2 == 0) -2 else 1
            CharacterAction.WATER, CharacterAction.GIFT -> -1
            CharacterAction.HAPPY -> -2
            else -> 0
        }

        when (dir) {
            Direction.DOWN, Direction.UP -> {
                val leftArmY = by + 1 - legSwing.coerceAtMost(1)
                val rightArmY = by + 1 + armRaise + legSwing.coerceAtLeast(-1)
                c.rect(torsoX - 2, leftArmY, 2, 5, skin.base)
                c.rect(torsoX - 2, leftArmY + 4, 2, 1, skin.shadow)
                c.rect(torsoX + torsoW, rightArmY, 2, 5, skin.base)
                c.rect(torsoX + torsoW, rightArmY + 4, 2, 1, skin.shadow)
            }
            Direction.LEFT -> {
                c.rect(9, by + 1 + armRaise, 3, 5, skin.base)
            }
            Direction.RIGHT -> {
                c.rect(12, by + 1 + armRaise, 3, 5, skin.base)
            }
        }

        // Legs
        if (action == CharacterAction.SIT) {
            c.rect(8, by + 6, 3, 3, skin.base)
            c.rect(13, by + 6, 3, 3, skin.base)
        } else {
            val lLegY = by + 7 + (if (legSwing > 0) -1 else 0)
            val rLegY = by + 7 + (if (legSwing < 0) -1 else 0)
            when (dir) {
                Direction.DOWN, Direction.UP -> {
                    c.rect(9, lLegY, 2, 4, skin.base)
                    c.rect(13, rLegY, 2, 4, skin.base)
                }
                Direction.LEFT, Direction.RIGHT -> {
                    c.rect(10 + legSwing, by + 7, 2, 4, skin.base)
                    c.rect(12 - legSwing, by + 7, 2, 4, skin.shadow)
                }
            }
        }
    }

    private fun drawShoes(
        c: PixelCanvas,
        shoesId: String,
        dir: Direction,
        action: CharacterAction,
        legSwing: Int,
        by: Int
    ) {
        val (shoeMain, shoeTrim) = when (shoesId) {
            "shoe_sneaker" -> Color.rgb(245, 240, 245) to Color.rgb(235, 138, 160)
            "shoe_maryjane" -> Color.rgb(112, 68, 48) to Color.rgb(74, 42, 28)
            else -> Color.rgb(138, 86, 52) to Color.rgb(92, 54, 32)
        }

        if (action == CharacterAction.SIT) {
            c.rect(8, by + 8, 3, 2, shoeMain)
            c.rect(13, by + 8, 3, 2, shoeMain)
            return
        }

        val ly = by + 10 + (if (legSwing > 0) -1 else 0)
        val ry = by + 10 + (if (legSwing < 0) -1 else 0)
        when (dir) {
            Direction.DOWN, Direction.UP -> {
                c.rect(8, ly, 3, 2, shoeMain)
                c.rect(8, ly + 1, 3, 1, shoeTrim)
                c.rect(13, ry, 3, 2, shoeMain)
                c.rect(13, ry + 1, 3, 1, shoeTrim)
            }
            Direction.LEFT -> {
                c.rect(9 + legSwing, by + 10, 3, 2, shoeMain)
                c.rect(11 - legSwing, by + 10, 3, 2, shoeTrim)
            }
            Direction.RIGHT -> {
                c.rect(10 + legSwing, by + 10, 3, 2, shoeMain)
                c.rect(12 - legSwing, by + 10, 3, 2, shoeTrim)
            }
        }
    }

    private fun drawBottom(
        c: PixelCanvas,
        bottomId: String,
        gender: Gender,
        dir: Direction,
        action: CharacterAction,
        legSwing: Int,
        by: Int,
        ramp: ColorRamp
    ) {
        val isSkirt = bottomId.contains("skirt") || bottomId == "bot_date_outfit" && gender == Gender.FEMALE
        val isShorts = bottomId == "bot_shorts"

        if (isSkirt) {
            c.rect(8, by + 5, 8, 3, ramp.base)
            c.rect(7, by + 6, 10, 3, ramp.base)
            c.rect(7, by + 8, 10, 1, ramp.shadow)
            if (bottomId == "bot_skirt_floral") {
                c.set(8, by + 7, Color.rgb(255, 232, 148))
                c.set(11, by + 6, Color.rgb(255, 248, 250))
                c.set(14, by + 7, Color.rgb(255, 232, 148))
            } else if (bottomId == "bot_skirt_casual") {
                c.set(9, by + 6, ramp.shadow)
                c.set(12, by + 6, ramp.shadow)
                c.set(15, by + 6, ramp.shadow)
            }
            return
        }

        val legLen = if (isShorts) 2 else 4
        c.rect(8, by + 5, 8, 2, ramp.base)
        if (action == CharacterAction.SIT) {
            c.rect(8, by + 6, 3, 2, ramp.base)
            c.rect(13, by + 6, 3, 2, ramp.base)
            return
        }
        when (dir) {
            Direction.DOWN, Direction.UP -> {
                val ly = by + 7 + (if (legSwing > 0) -1 else 0)
                val ry = by + 7 + (if (legSwing < 0) -1 else 0)
                c.rect(9, ly, 2, legLen, ramp.base)
                c.rect(13, ry, 2, legLen, ramp.base)
                c.rect(9, ly + legLen - 1, 2, 1, ramp.shadow)
                c.rect(13, ry + legLen - 1, 2, 1, ramp.shadow)
            }
            Direction.LEFT, Direction.RIGHT -> {
                c.rect(9 + legSwing, by + 7, 3, legLen, ramp.base)
                c.rect(11 - legSwing, by + 7, 3, legLen, ramp.shadow)
            }
        }
    }

    private fun drawTop(
        c: PixelCanvas,
        topId: String,
        gender: Gender,
        dir: Direction,
        action: CharacterAction,
        frame: Int,
        by: Int,
        ramp: ColorRamp,
        botRamp: ColorRamp
    ) {
        val torsoW = if (gender == Gender.MALE) 8 else 6
        val torsoX = 12 - torsoW / 2
        c.rect(torsoX, by, torsoW, 6, ramp.base)
        c.rect(torsoX, by, torsoW, 1, ramp.highlight)
        c.rect(torsoX, by + 5, torsoW, 1, ramp.shadow)

        // Sleeves
        val isShortSleeve = topId == "top_tee_sunny" || topId == "top_picnic_vest"
        val sleeveLen = if (isShortSleeve) 2 else 4
        if (dir == Direction.DOWN || dir == Direction.UP) {
            c.rect(torsoX - 2, by + 1, 2, sleeveLen, ramp.base)
            c.rect(torsoX + torsoW, by + 1, 2, sleeveLen, ramp.base)
        } else if (dir == Direction.LEFT) {
            c.rect(9, by + 1, 3, sleeveLen, ramp.highlight)
        } else {
            c.rect(12, by + 1, 3, sleeveLen, ramp.highlight)
        }

        // Style details
        when (topId) {
            "top_shirt_white" -> {
                if (dir == Direction.DOWN) {
                    c.set(11, by, Color.rgb(255, 255, 255))
                    c.set(12, by, Color.rgb(255, 255, 255))
                    c.set(11, by + 2, ramp.shadow)
                    c.set(11, by + 4, ramp.shadow)
                }
            }
            "top_sweater_pastel", "top_winter_knit" -> {
                c.rect(torsoX, by + 4, torsoW, 2, ramp.shadow)
                if (dir == Direction.DOWN) {
                    c.rect(10, by, 4, 1, ramp.highlight)
                }
            }
            "top_hoodie_cute" -> {
                c.rect(torsoX - 1, by, torsoW + 2, 2, ramp.shadow)
                if (dir == Direction.DOWN) {
                    c.rect(10, by + 3, 4, 2, ramp.highlight)
                }
            }
            "top_picnic_vest" -> {
                c.rect(torsoX, by, 2, 5, botRamp.shadow)
                c.rect(torsoX + torsoW - 2, by, 2, 5, botRamp.shadow)
            }
            "top_pajama" -> {
                if (dir == Direction.DOWN) {
                    c.set(10, by + 2, Color.rgb(255, 236, 128))
                    c.set(13, by + 3, Color.rgb(255, 236, 128))
                }
            }
            "top_date_coat", "top_festival_hanbok" -> {
                c.rect(torsoX, by + 4, torsoW, 3, ramp.base)
                if (dir == Direction.DOWN) {
                    c.rect(11, by + 1, 2, 5, Color.rgb(252, 224, 152))
                }
            }
        }
    }

    private fun drawOverallsStraps(c: PixelCanvas, dir: Direction, by: Int, ramp: ColorRamp) {
        if (dir == Direction.DOWN || dir == Direction.UP) {
            c.rect(9, by, 1, 5, ramp.base)
            c.rect(14, by, 1, 5, ramp.base)
            c.rect(9, by + 3, 6, 3, ramp.base)
            if (dir == Direction.DOWN) {
                c.set(9, by + 3, Color.rgb(245, 206, 98))
                c.set(14, by + 3, Color.rgb(245, 206, 98))
            }
        } else {
            c.rect(10, by, 4, 5, ramp.base)
        }
    }

    private fun drawActionProp(c: PixelCanvas, action: CharacterAction, dir: Direction, frame: Int, by: Int) {
        when (action) {
            CharacterAction.HOE -> {
                val hx = when (dir) {
                    Direction.LEFT -> 4
                    Direction.RIGHT -> 18
                    else -> 16
                }
                val hy = by - 2 + (frame % 2) * 3
                c.rect(hx, hy, 1, 6, Color.rgb(142, 92, 52))
                c.rect(hx - 1, hy, 3, 2, Color.rgb(165, 182, 198))
            }
            CharacterAction.WATER -> {
                val wx = when (dir) {
                    Direction.LEFT -> 4
                    Direction.RIGHT -> 17
                    else -> 15
                }
                c.rect(wx, by + 1, 4, 3, Color.rgb(122, 204, 182))
                c.rect(wx + (if (dir == Direction.LEFT) -1 else 4), by + 1, 1, 2, Color.rgb(92, 174, 152))
                if (frame % 2 == 1) {
                    val dropX = if (dir == Direction.LEFT) wx - 2 else wx + 5
                    c.set(dropX, by + 4, Color.rgb(138, 218, 252))
                    c.set(dropX, by + 6, Color.rgb(138, 218, 252))
                }
            }
            CharacterAction.GIFT -> {
                c.rect(10, by - 2, 4, 4, Color.rgb(245, 146, 174))
                c.rect(11, by - 2, 2, 4, Color.rgb(255, 224, 132))
            }
            else -> {}
        }
    }

    private fun drawFaceAndEyes(
        c: PixelCanvas,
        dir: Direction,
        hy: Int,
        skin: ColorRamp,
        eyeStyle: EyeStyle,
        eyeColor: Int,
        exp: Expression,
        blink: Boolean,
        browColor: Int
    ) {
        val (lx, rx) = when (dir) {
            Direction.DOWN -> 8 to 14
            Direction.LEFT -> 6 to 10
            Direction.RIGHT -> 12 to 16
            Direction.UP -> return
        }

        // Eyebrows
        c.rect(lx, hy + 5, 2, 1, browColor)
        c.rect(rx, hy + 5, 2, 1, browColor)

        // Eyes
        if (blink || exp == Expression.HAPPY) {
            // Happy / closed arc
            c.rect(lx, hy + 7, 2, 1, PixelPalette.OUTLINE)
            c.rect(rx, hy + 7, 2, 1, PixelPalette.OUTLINE)
        } else {
            c.rect(lx, hy + 7, 2, 2, eyeColor)
            c.rect(rx, hy + 7, 2, 2, eyeColor)
            if (eyeStyle == EyeStyle.SPARKLE || eyeStyle == EyeStyle.ROUND) {
                c.set(lx, hy + 7, PixelPalette.WHITE_HIGHLIGHT)
                c.set(rx, hy + 7, PixelPalette.WHITE_HIGHLIGHT)
            }
            if (eyeStyle == EyeStyle.GENTLE) {
                c.rect(lx, hy + 6, 2, 1, PixelPalette.OUTLINE)
                c.rect(rx, hy + 6, 2, 1, PixelPalette.OUTLINE)
            }
        }

        // Blush
        if (exp == Expression.SHY || exp == Expression.LOVE || exp == Expression.HAPPY) {
            c.rect(lx - 1, hy + 9, 2, 1, PixelPalette.BLUSH)
            c.rect(rx + 1, hy + 9, 2, 1, PixelPalette.BLUSH)
        } else {
            c.set(lx - 1, hy + 9, PixelPalette.BLUSH)
            c.set(rx + 2, hy + 9, PixelPalette.BLUSH)
        }

        // Mouth
        val mx = when (dir) {
            Direction.DOWN -> 11
            Direction.LEFT -> 8
            Direction.RIGHT -> 14
            Direction.UP -> 11
        }
        when (exp) {
            Expression.HAPPY, Expression.LOVE -> {
                c.rect(mx, hy + 10, 2, 1, Color.rgb(206, 98, 108))
            }
            Expression.SURPRISED -> {
                c.set(mx, hy + 10, Color.rgb(186, 86, 96))
            }
            Expression.SAD -> {
                c.rect(mx, hy + 10, 2, 1, skin.shadow)
            }
            else -> {
                c.set(mx, hy + 10, skin.shadow)
            }
        }
    }

    private fun drawHairBackLayer(
        c: PixelCanvas,
        hairId: String,
        dir: Direction,
        hy: Int,
        hair: ColorRamp
    ) {
        when (hairId) {
            "f_long_straight", "f_layer", "f_two_part", "m_long", "f_anime" -> {
                c.rect(5, hy + 4, 14, 13, hair.shadow)
                c.rect(6, hy + 4, 12, 12, hair.base)
            }
            "f_curly" -> {
                c.rect(4, hy + 4, 16, 13, hair.shadow)
                c.rect(5, hy + 4, 14, 12, hair.base)
            }
            "f_bob", "f_flat_bangs" -> {
                c.rect(5, hy + 4, 14, 8, hair.shadow)
                c.rect(6, hy + 4, 12, 7, hair.base)
            }
            "f_braid" -> {
                c.rect(10, hy + 10, 4, 8, hair.shadow)
                c.rect(11, hy + 10, 2, 8, hair.base)
            }
            "f_ponytail", "m_tied_high" -> {
                val px = when (dir) {
                    Direction.LEFT -> 16
                    Direction.RIGHT -> 5
                    else -> 10
                }
                c.rect(px, hy + 2, 3, 10, hair.shadow)
                c.rect(px + 1, hy + 2, 2, 9, hair.base)
            }
            "f_tied_low", "m_tied_low" -> {
                val px = when (dir) {
                    Direction.LEFT -> 15
                    Direction.RIGHT -> 6
                    else -> 10
                }
                c.rect(px, hy + 7, 3, 7, hair.shadow)
            }
        }
    }

    private fun drawHairFrontAndTop(
        c: PixelCanvas,
        hairId: String,
        dir: Direction,
        hy: Int,
        hair: ColorRamp
    ) {
        // Crown base
        c.rect(6, hy - 1, 12, 4, hair.base)
        c.rect(7, hy - 2, 10, 2, hair.base)
        c.rect(8, hy - 1, 8, 1, hair.highlight)

        if (dir == Direction.UP) {
            // Full back-of-head hair coverage
            c.rect(6, hy + 1, 12, 11, hair.base)
            c.rect(7, hy + 2, 10, 2, hair.highlight)
            when (hairId) {
                "f_long_straight", "f_layer", "f_two_part", "f_curly", "m_long", "f_anime" -> {
                    c.rect(6, hy + 10, 12, 7, hair.base)
                    c.rect(7, hy + 15, 10, 2, hair.shadow)
                }
                "f_bob", "f_flat_bangs" -> {
                    c.rect(6, hy + 10, 12, 3, hair.shadow)
                }
                "f_ponytail", "m_tied_high" -> {
                    c.rect(10, hy + 3, 4, 12, hair.shadow)
                    c.rect(11, hy + 3, 2, 11, hair.highlight)
                    c.rect(10, hy + 2, 4, 1, Color.rgb(242, 142, 168))
                }
                "f_tied_low", "m_tied_low" -> {
                    c.rect(10, hy + 9, 4, 6, hair.shadow)
                    c.rect(10, hy + 8, 4, 1, Color.rgb(242, 142, 168))
                }
                "f_braid" -> {
                    c.rect(10, hy + 8, 4, 9, hair.shadow)
                    c.rect(11, hy + 9, 2, 7, hair.highlight)
                }
                "f_bun" -> {
                    c.rect(9, hy - 5, 6, 4, hair.base)
                    c.rect(10, hy - 4, 4, 2, hair.highlight)
                }
                "f_twin_bun" -> {
                    c.rect(5, hy - 4, 4, 4, hair.base)
                    c.rect(15, hy - 4, 4, 4, hair.base)
                }
                "m_undercut" -> {
                    c.rect(7, hy + 8, 10, 3, hair.shadow)
                }
            }
            return
        }

        // Side locks
        when (dir) {
            Direction.DOWN -> {
                c.rect(5, hy + 1, 2, 6, hair.base)
                c.rect(17, hy + 1, 2, 6, hair.base)
            }
            Direction.LEFT -> {
                c.rect(14, hy + 1, 4, 10, hair.base)
                c.rect(16, hy + 3, 2, 7, hair.shadow)
            }
            Direction.RIGHT -> {
                c.rect(6, hy + 1, 4, 10, hair.base)
                c.rect(6, hy + 3, 2, 7, hair.shadow)
            }
            Direction.UP -> {}
        }

        // Bangs & style crown silhouettes (12 Male + 12 Female)
        when (hairId) {
            "m_short" -> {
                c.rect(7, hy + 2, 10, 2, hair.base)
            }
            "m_bangs", "f_flat_bangs" -> {
                c.rect(6, hy + 2, 12, 3, hair.base)
                c.rect(7, hy + 4, 10, 1, hair.shadow)
            }
            "m_curly", "f_curly" -> {
                c.rect(5, hy - 2, 14, 4, hair.base)
                c.set(6, hy - 3, hair.highlight)
                c.set(11, hy - 3, hair.highlight)
                c.set(16, hy - 3, hair.highlight)
                c.rect(6, hy + 2, 3, 3, hair.base)
                c.rect(15, hy + 2, 3, 3, hair.base)
            }
            "m_layer", "m_k_layer", "f_layer" -> {
                c.rect(6, hy + 2, 5, 3, hair.base)
                c.rect(13, hy + 2, 5, 3, hair.base)
                c.rect(9, hy + 2, 6, 2, hair.highlight)
            }
            "m_parted", "m_two_part", "f_two_part" -> {
                c.rect(6, hy + 2, 4, 4, hair.base)
                c.rect(14, hy + 2, 4, 4, hair.base)
                c.set(11, hy + 1, hair.shadow)
            }
            "m_undercut" -> {
                c.rect(7, hy - 3, 10, 3, hair.highlight)
                c.rect(6, hy + 2, 12, 2, hair.base)
            }
            "m_anime", "f_anime" -> {
                c.rect(6, hy + 2, 3, 5, hair.base)
                c.rect(10, hy + 2, 4, 4, hair.highlight)
                c.rect(15, hy + 2, 3, 5, hair.base)
                c.set(11, hy - 3, hair.highlight)
                c.set(12, hy - 4, hair.highlight)
            }
            "f_bun" -> {
                c.rect(9, hy - 5, 6, 4, hair.base)
                c.rect(10, hy - 4, 4, 2, hair.highlight)
                c.rect(6, hy + 2, 12, 2, hair.base)
            }
            "f_twin_bun" -> {
                c.rect(5, hy - 4, 4, 4, hair.base)
                c.rect(15, hy - 4, 4, 4, hair.base)
                c.rect(7, hy + 2, 10, 2, hair.base)
            }
            "f_ponytail", "m_tied_high" -> {
                c.rect(10, hy - 4, 4, 2, hair.highlight)
                c.rect(7, hy + 2, 10, 2, hair.base)
            }
            else -> {
                c.rect(6, hy + 2, 12, 2, hair.base)
            }
        }
    }

    private fun drawHat(c: PixelCanvas, hatId: String, dir: Direction, hy: Int) {
        when (hatId) {
            "hat_straw" -> {
                val straw = Color.rgb(242, 206, 124)
                val strawShade = Color.rgb(202, 162, 82)
                val band = Color.rgb(228, 114, 134)
                c.rect(3, hy, 18, 2, straw)
                c.rect(6, hy - 3, 12, 3, straw)
                c.rect(6, hy - 1, 12, 1, band)
                c.rect(4, hy + 1, 16, 1, strawShade)
            }
            "hat_beret" -> {
                val beret = Color.rgb(198, 92, 114)
                val shade = Color.rgb(152, 64, 86)
                c.rect(6, hy - 3, 12, 3, beret)
                c.rect(11, hy - 4, 2, 1, shade)
                c.rect(5, hy - 1, 14, 1, shade)
            }
            "hat_flower_crown" -> {
                c.rect(6, hy, 12, 2, Color.rgb(104, 174, 108))
                c.set(7, hy, Color.rgb(255, 182, 204))
                c.set(11, hy, Color.rgb(255, 236, 132))
                c.set(15, hy, Color.rgb(255, 182, 204))
            }
            "hat_beanie" -> {
                val knit = Color.rgb(162, 206, 232)
                val cuff = Color.rgb(124, 174, 206)
                c.rect(6, hy - 3, 12, 3, knit)
                c.rect(5, hy - 1, 14, 2, cuff)
                c.rect(11, hy - 4, 2, 1, Color.WHITE)
            }
        }
    }

    private fun drawAccessory(c: PixelCanvas, accId: String, dir: Direction, hy: Int, by: Int) {
        when (accId) {
            "acc_glasses" -> {
                if (dir == Direction.DOWN) {
                    val frame = Color.rgb(88, 62, 48)
                    c.rect(7, hy + 6, 4, 1, frame)
                    c.rect(7, hy + 9, 4, 1, frame)
                    c.set(11, hy + 7, frame)
                    c.rect(13, hy + 6, 4, 1, frame)
                    c.rect(13, hy + 9, 4, 1, frame)
                }
            }
            "acc_scarf" -> {
                val scarf = Color.rgb(236, 134, 152)
                val scarfShade = Color.rgb(196, 94, 114)
                c.rect(8, by - 1, 8, 2, scarf)
                if (dir == Direction.DOWN) {
                    c.rect(13, by + 1, 2, 4, scarfShade)
                }
            }
            "acc_ribbon" -> {
                if (dir != Direction.UP) {
                    c.rect(15, hy + 1, 3, 2, Color.rgb(245, 122, 152))
                }
            }
            "acc_bag" -> {
                val strap = Color.rgb(138, 86, 52)
                c.set(9, by + 1, strap)
                c.set(10, by + 2, strap)
                c.set(11, by + 3, strap)
                c.rect(13, by + 4, 3, 3, Color.rgb(184, 124, 78))
            }
        }
    }

    private fun applySoftOutline(c: PixelCanvas) {
        val copy = c.pixels.copyOf()
        for (y in 1 until c.h - 1) {
            for (x in 1 until c.w - 1) {
                if (copy[y * c.w + x] == Color.TRANSPARENT) {
                    val hasNeighbor =
                        (copy[(y - 1) * c.w + x] != Color.TRANSPARENT && Color.alpha(copy[(y - 1) * c.w + x]) > 200) ||
                        (copy[(y + 1) * c.w + x] != Color.TRANSPARENT && Color.alpha(copy[(y + 1) * c.w + x]) > 200) ||
                        (copy[y * c.w + (x - 1)] != Color.TRANSPARENT && Color.alpha(copy[y * c.w + (x - 1)]) > 200) ||
                        (copy[y * c.w + (x + 1)] != Color.TRANSPARENT && Color.alpha(copy[y * c.w + (x + 1)]) > 200)
                    if (hasNeighbor) {
                        c.set(x, y, PixelPalette.OUTLINE)
                    }
                }
            }
        }
    }

    private fun renderPortraitBitmap(app: CharacterAppearance, blink: Boolean): Bitmap {
        val c = PixelCanvas(32, 32)
        val skin = PixelPalette.skinRamps[app.skinToneIndex.coerceIn(PixelPalette.skinRamps.indices)]
        val hair = PixelPalette.hairRamps[app.hairColorIndex.coerceIn(PixelPalette.hairRamps.indices)]
        val topRamp = PixelPalette.clothRamps[app.topColorIndex.coerceIn(PixelPalette.clothRamps.indices)]
        val eyeColor = PixelPalette.eyeColors[app.eyeColorIndex.coerceIn(PixelPalette.eyeColors.indices)].second

        // Soft pastel portrait backdrop
        c.rect(0, 0, 32, 32, Color.rgb(252, 240, 224))
        c.rect(1, 1, 30, 30, Color.rgb(255, 248, 236))

        // Back hair
        if (app.hairStyleId.startsWith("f_") && app.hairStyleId != "f_bob" && app.hairStyleId != "f_bun" || app.hairStyleId == "m_long") {
            c.rect(6, 10, 20, 21, hair.shadow)
            c.rect(7, 10, 18, 20, hair.base)
        }

        // Shoulders & shirt
        c.rect(7, 25, 18, 7, topRamp.base)
        c.rect(8, 25, 16, 2, topRamp.highlight)
        c.rect(13, 22, 6, 4, skin.shadow)

        // Head
        c.rect(9, 7, 14, 16, skin.base)
        c.rect(8, 9, 16, 12, skin.base)
        c.rect(10, 8, 12, 2, skin.highlight)
        // Ears
        c.rect(7, 14, 2, 4, skin.shadow)
        c.rect(23, 14, 2, 4, skin.shadow)

        // Eyebrows
        val browY = when (app.eyebrowStyle) {
            EyebrowStyle.ARCHED -> 12
            else -> 13
        }
        c.rect(10, browY, 4, 1, hair.shadow)
        c.rect(18, browY, 4, 1, hair.shadow)

        // Eyes
        if (blink || app.expression == Expression.HAPPY) {
            c.rect(10, 16, 4, 1, PixelPalette.OUTLINE)
            c.rect(18, 16, 4, 1, PixelPalette.OUTLINE)
        } else {
            c.rect(10, 15, 4, 3, Color.WHITE)
            c.rect(18, 15, 4, 3, Color.WHITE)
            c.rect(11, 15, 3, 3, eyeColor)
            c.rect(18, 15, 3, 3, eyeColor)
            c.set(11, 15, PixelPalette.WHITE_HIGHLIGHT)
            c.set(18, 15, PixelPalette.WHITE_HIGHLIGHT)
            c.rect(10, 14, 4, 1, PixelPalette.OUTLINE)
            c.rect(18, 14, 4, 1, PixelPalette.OUTLINE)
        }

        // Blush
        val blushWidth = if (app.expression == Expression.SHY || app.expression == Expression.LOVE) 4 else 2
        c.rect(9, 19, blushWidth, 1, PixelPalette.BLUSH)
        c.rect(23 - blushWidth, 19, blushWidth, 1, PixelPalette.BLUSH)

        // Mouth
        when (app.expression) {
            Expression.HAPPY, Expression.LOVE -> {
                c.rect(14, 20, 4, 1, Color.rgb(212, 96, 112))
                c.rect(15, 21, 2, 1, Color.rgb(245, 146, 162))
            }
            Expression.SHY -> {
                c.rect(14, 20, 3, 1, Color.rgb(212, 96, 112))
            }
            Expression.SAD -> {
                c.rect(14, 21, 4, 1, skin.shadow)
                c.set(13, 22, skin.shadow)
                c.set(18, 22, skin.shadow)
            }
            Expression.SURPRISED -> {
                c.rect(15, 20, 2, 2, Color.rgb(206, 92, 108))
            }
            Expression.NEUTRAL -> {
                c.rect(15, 20, 3, 1, skin.shadow)
            }
        }

        // Hair crown + bangs
        c.rect(8, 5, 16, 6, hair.base)
        c.rect(9, 4, 14, 3, hair.base)
        c.rect(10, 5, 12, 2, hair.highlight)
        c.rect(7, 9, 2, 9, hair.base)
        c.rect(23, 9, 2, 9, hair.base)
        when (app.hairStyleId) {
            "f_flat_bangs", "m_bangs" -> c.rect(9, 9, 14, 4, hair.base)
            "m_parted", "m_two_part", "f_two_part" -> {
                c.rect(9, 9, 5, 4, hair.base)
                c.rect(18, 9, 5, 4, hair.base)
            }
            "f_bun", "f_ponytail", "m_tied_high" -> {
                c.rect(12, 1, 8, 4, hair.base)
                c.rect(9, 9, 14, 3, hair.base)
            }
            else -> c.rect(9, 9, 14, 3, hair.base)
        }

        if (app.accessoryId == "acc_glasses") {
            val g = Color.rgb(88, 62, 48)
            c.rect(9, 14, 6, 1, g)
            c.rect(9, 18, 6, 1, g)
            c.rect(15, 16, 2, 1, g)
            c.rect(17, 14, 6, 1, g)
            c.rect(17, 18, 6, 1, g)
        }
        if (app.hatId == "hat_straw") {
            c.rect(4, 6, 24, 2, Color.rgb(242, 206, 124))
            c.rect(8, 2, 16, 4, Color.rgb(242, 206, 124))
            c.rect(8, 5, 16, 1, Color.rgb(228, 114, 134))
        } else if (app.hatId == "hat_beret") {
            c.rect(7, 2, 18, 5, Color.rgb(198, 92, 114))
        } else if (app.hatId == "hat_flower_crown") {
            c.rect(8, 6, 16, 2, Color.rgb(104, 174, 108))
            c.rect(10, 6, 2, 2, Color.rgb(255, 182, 204))
            c.rect(15, 6, 2, 2, Color.rgb(255, 236, 132))
            c.rect(20, 6, 2, 2, Color.rgb(255, 182, 204))
        }

        if (app.expression == Expression.LOVE) {
            c.set(26, 5, Color.rgb(242, 92, 126))
            c.set(28, 5, Color.rgb(242, 92, 126))
            c.rect(26, 6, 3, 1, Color.rgb(242, 92, 126))
            c.set(27, 7, Color.rgb(242, 92, 126))
        }

        return c.toBitmap()
    }
}
