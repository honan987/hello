package com.example.game.pixel

import android.graphics.Color as AndroidColor

data class ColorRamp(
    val nameVi: String,
    val highlight: Int,
    val base: Int,
    val shadow: Int
)

object PixelPalette {
    val OUTLINE = AndroidColor.rgb(59, 40, 32) // Soft warm dark brown outline (#3B2820)
    val OUTLINE_SOFT = AndroidColor.rgb(92, 64, 51)
    val BLUSH = AndroidColor.rgb(245, 146, 168)
    val WHITE_HIGHLIGHT = AndroidColor.rgb(255, 252, 245)

    // 6 Skin tone ramps
    val skinRamps = listOf(
        ColorRamp("Trắng hồng", AndroidColor.rgb(255, 232, 222), AndroidColor.rgb(250, 212, 196), AndroidColor.rgb(227, 174, 154)),
        ColorRamp("Sáng ấm", AndroidColor.rgb(255, 226, 204), AndroidColor.rgb(244, 198, 168), AndroidColor.rgb(212, 158, 128)),
        ColorRamp("Mật ong dịu", AndroidColor.rgb(240, 198, 160), AndroidColor.rgb(220, 172, 132), AndroidColor.rgb(184, 134, 96)),
        ColorRamp("Nắng vàng", AndroidColor.rgb(224, 176, 130), AndroidColor.rgb(198, 148, 102), AndroidColor.rgb(160, 112, 72)),
        ColorRamp("Nâu ấm", AndroidColor.rgb(186, 134, 98), AndroidColor.rgb(156, 106, 72), AndroidColor.rgb(122, 78, 48)),
        ColorRamp("Socola", AndroidColor.rgb(142, 98, 72), AndroidColor.rgb(112, 74, 52), AndroidColor.rgb(82, 52, 36))
    )

    // 10 Hair color ramps (8+ required)
    val hairRamps = listOf(
        ColorRamp("Đen huyền", AndroidColor.rgb(74, 70, 88), AndroidColor.rgb(46, 42, 56), AndroidColor.rgb(28, 24, 36)),
        ColorRamp("Nâu hạt dẻ", AndroidColor.rgb(142, 96, 72), AndroidColor.rgb(106, 68, 48), AndroidColor.rgb(74, 46, 32)),
        ColorRamp("Nâu sáng", AndroidColor.rgb(192, 142, 100), AndroidColor.rgb(154, 108, 72), AndroidColor.rgb(116, 78, 50)),
        ColorRamp("Vàng nắng", AndroidColor.rgb(252, 226, 140), AndroidColor.rgb(232, 192, 94), AndroidColor.rgb(188, 144, 58)),
        ColorRamp("Bạch kim", AndroidColor.rgb(250, 248, 255), AndroidColor.rgb(224, 222, 236), AndroidColor.rgb(182, 180, 204)),
        ColorRamp("Hồng pastel", AndroidColor.rgb(252, 204, 222), AndroidColor.rgb(238, 162, 190), AndroidColor.rgb(198, 120, 152)),
        ColorRamp("Xanh pastel", AndroidColor.rgb(182, 232, 242), AndroidColor.rgb(132, 198, 218), AndroidColor.rgb(92, 156, 182)),
        ColorRamp("Tím nhạt", AndroidColor.rgb(218, 196, 246), AndroidColor.rgb(182, 152, 222), AndroidColor.rgb(140, 112, 186)),
        ColorRamp("Cam đào", AndroidColor.rgb(252, 194, 156), AndroidColor.rgb(236, 152, 112), AndroidColor.rgb(194, 112, 78)),
        ColorRamp("Xanh rêu", AndroidColor.rgb(164, 214, 174), AndroidColor.rgb(116, 174, 132), AndroidColor.rgb(78, 132, 94))
    )

    // 8 Eye colors
    val eyeColors = listOf(
        "Nâu sẫm" to AndroidColor.rgb(72, 46, 36),
        "Hổ phách" to AndroidColor.rgb(168, 108, 48),
        "Xanh biển" to AndroidColor.rgb(66, 136, 196),
        "Xanh ngọc" to AndroidColor.rgb(68, 166, 138),
        "Tím thạch anh" to AndroidColor.rgb(138, 96, 182),
        "Hồng anh đào" to AndroidColor.rgb(212, 102, 144),
        "Xám khói" to AndroidColor.rgb(104, 116, 132),
        "Vàng nắng" to AndroidColor.rgb(206, 162, 54)
    )

    // 10 Clothing color ramps
    val clothRamps = listOf(
        ColorRamp("Kem trắng", AndroidColor.rgb(255, 250, 240), AndroidColor.rgb(236, 226, 210), AndroidColor.rgb(198, 184, 166)),
        ColorRamp("Xanh denim", AndroidColor.rgb(122, 164, 206), AndroidColor.rgb(86, 126, 174), AndroidColor.rgb(58, 90, 134)),
        ColorRamp("Mint dịu", AndroidColor.rgb(184, 238, 212), AndroidColor.rgb(136, 208, 176), AndroidColor.rgb(94, 166, 136)),
        ColorRamp("Hồng phấn", AndroidColor.rgb(252, 198, 214), AndroidColor.rgb(238, 156, 180), AndroidColor.rgb(196, 114, 142)),
        ColorRamp("Vàng bơ", AndroidColor.rgb(252, 232, 164), AndroidColor.rgb(236, 202, 114), AndroidColor.rgb(194, 156, 74)),
        ColorRamp("Xanh trời", AndroidColor.rgb(174, 222, 246), AndroidColor.rgb(124, 184, 222), AndroidColor.rgb(86, 142, 184)),
        ColorRamp("Nâu gỗ", AndroidColor.rgb(174, 126, 92), AndroidColor.rgb(138, 92, 62), AndroidColor.rgb(102, 64, 42)),
        ColorRamp("Tím oải hương", AndroidColor.rgb(214, 192, 242), AndroidColor.rgb(174, 146, 212), AndroidColor.rgb(134, 108, 174)),
        ColorRamp("Đỏ dâu", AndroidColor.rgb(242, 136, 148), AndroidColor.rgb(212, 92, 108), AndroidColor.rgb(168, 62, 78)),
        ColorRamp("Xanh lá dịu", AndroidColor.rgb(166, 218, 152), AndroidColor.rgb(122, 182, 110), AndroidColor.rgb(84, 140, 76))
    )

    // Environment colors
    val GRASS_LIGHT_A = AndroidColor.rgb(156, 212, 128)
    val GRASS_LIGHT_B = AndroidColor.rgb(144, 202, 118)
    val GRASS_MED_A = AndroidColor.rgb(130, 192, 108)
    val GRASS_MED_B = AndroidColor.rgb(120, 180, 98)
    val DIRT_A = AndroidColor.rgb(214, 174, 130)
    val DIRT_B = AndroidColor.rgb(198, 158, 114)
    val SOIL_DRY = AndroidColor.rgb(164, 116, 78)
    val SOIL_TILLED = AndroidColor.rgb(132, 86, 54)
    val SOIL_WATERED = AndroidColor.rgb(88, 56, 38)
    val WATER_A = AndroidColor.rgb(112, 194, 226)
    val WATER_B = AndroidColor.rgb(94, 178, 214)
    val WATER_FOAM = AndroidColor.rgb(214, 244, 255)
    val WOOD_LIGHT = AndroidColor.rgb(186, 132, 88)
    val WOOD_DARK = AndroidColor.rgb(138, 92, 56)
}
