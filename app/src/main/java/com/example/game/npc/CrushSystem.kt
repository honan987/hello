package com.example.game.npc

import com.example.game.model.*

data class CrushScheduleSlot(
    val startHour: Int,
    val targetX: Float,
    val targetY: Float,
    val activityAction: CharacterAction,
    val facing: Direction,
    val activityTextVi: String
)

object CrushSystem {

    val dailySchedule = listOf(
        CrushScheduleSlot(6, 25f, 8.5f, CharacterAction.WATER, Direction.DOWN, "Đang tưới hoa buổi sáng"),
        CrushScheduleSlot(9, 10f, 19.5f, CharacterAction.WALK, Direction.DOWN, "Đang dạo quanh đồng hoa"),
        CrushScheduleSlot(12, 16f, 10.2f, CharacterAction.SIT, Direction.DOWN, "Đang đọc sách ở khu vườn"),
        CrushScheduleSlot(15, 27.5f, 23.2f, CharacterAction.SIT, Direction.UP, "Đang ngồi bên hồ ngắm hoàng hôn"),
        CrushScheduleSlot(19, 25.5f, 8.2f, CharacterAction.IDLE, Direction.DOWN, "Đang thư giãn trước hiên nhà")
    )

    fun currentScheduleFor(hour: Int): CrushScheduleSlot {
        return dailySchedule.lastOrNull { hour >= it.startHour } ?: dailySchedule.first()
    }

    fun crushName(gender: Gender, customName: String): String =
        customName.ifBlank { if (gender == Gender.MALE) "Minh" else "Linh" }

    fun buildDialogue(
        crushName: String,
        playerName: String,
        tier: RelationshipTier,
        timePhase: TimePhase,
        weather: WeatherType,
        zone: WorldZone,
        day: Int
    ): ActiveDialogue {
        val greeting = when (timePhase) {
            TimePhase.DAWN, TimePhase.MORNING -> "Chào buổi sáng, $playerName! Không khí hôm nay thật dễ chịu phải không?"
            TimePhase.NOON -> "Trưa rồi đó $playerName, nhớ nghỉ ngơi uống nước kẻo mệt nhé!"
            TimePhase.AFTERNOON -> "Buổi chiều nắng dịu quá, mình thích nhất khoảng thời gian này trong ngày."
            TimePhase.SUNSET -> "Nhìn kìa $playerName... Bầu trời hoàng hôn cam hồng soi xuống mặt hồ đẹp đến ngỡ ngàng."
            TimePhase.EVENING, TimePhase.NIGHT -> "Màn đêm xuống rồi, ánh đèn lồng với đom đóm làm khu vườn ấm áp hẳn lên."
        }

        val weatherLine = when (weather) {
            WeatherType.RAINY -> "Ngoài trời đang mưa lất phất, tiếng mưa rơi lên mái hiên nghe thật bình yên."
            WeatherType.RAINBOW -> "Sau cơn mưa có cầu vồng rực rỡ kìa! Cậu đã ước một điều gì chưa?"
            WeatherType.METEOR_SHOWER -> "Đêm nay có mưa sao băng đó! Ở bên cậu ngắm bầu trời thế này thật tuyệt."
            WeatherType.FOGGY -> "Sáng nay sương mù giăng nhẹ qua rừng thông, trông cứ như trong truyện cổ tích vậy."
            else -> ""
        }

        val tierFlavor = when (tier) {
            RelationshipTier.STRANGER -> "Rất vui được làm quen với hàng xóm mới. Khu vườn của cậu bắt đầu có sức sống rồi đấy!"
            RelationshipTier.ACQUAINTANCE -> "Dạo này thấy cậu chăm chỉ ngoài đồng lắm nha. Có cần mình phụ tưới hoa không?"
            RelationshipTier.FRIEND -> "Mỗi lần nói chuyện với $playerName, mình đều thấy tâm trạng nhẹ nhõm hẳn đi."
            RelationshipTier.CLOSE_FRIEND -> "Không biết từ lúc nào, việc gặp cậu mỗi ngày đã trở thành thói quen yêu thích nhất của mình."
            RelationshipTier.CRUSH -> "Ơ... sao tự nhiên tim mình đập nhanh thế này? Gặp được $playerName ở ${zone.nameVi} vui quá..."
            RelationshipTier.LOVE -> "$playerName à, dù nông trại có bận rộn thế nào, chỉ cần có cậu bên cạnh là đủ ấm áp cả bốn mùa rồi."
        }

        val fullText = buildString {
            append(greeting)
            if (weatherLine.isNotEmpty()) append("\n\n$weatherLine")
            append("\n\n$tierFlavor")
        }

        val options = when (tier) {
            RelationshipTier.STRANGER, RelationshipTier.ACQUAINTANCE -> listOf(
                DialogueOption(
                    labelVi = "Hỏi thăm công việc làm vườn",
                    responseVi = "Hôm nay hoa oải hương nở đều lắm! Nếu cậu thích, hôm nào mình chỉ cho cậu cách ươm giống nha.",
                    affectionDelta = 4,
                    crushExpression = Expression.HAPPY
                ),
                DialogueOption(
                    labelVi = "Khen cảnh sắc nơi đây thật yên bình",
                    responseVi = "Đúng vậy, khu vườn nhỏ này luôn chữa lành mọi mệt mỏi. Rất vui vì có cậu chuyển đến đây!",
                    affectionDelta = 5,
                    crushExpression = Expression.SHY
                ),
                DialogueOption(
                    labelVi = "Chào tạm biệt để tiếp tục trồng trọt",
                    responseVi = "Ừa, cậu làm việc vui vẻ nhé! Đừng quên ăn nhẹ khi hết năng lượng đó.",
                    affectionDelta = 2,
                    crushExpression = Expression.NEUTRAL
                )
            )
            RelationshipTier.FRIEND, RelationshipTier.CLOSE_FRIEND -> listOf(
                DialogueOption(
                    labelVi = "Rủ $crushName chiều nay ngắm hoàng hôn bên hồ",
                    responseVi = "Thật sao? Mình đồng ý ngay! Hoàng hôn phản chiếu trên mặt hồ nước cùng cậu chắc chắn sẽ rất đẹp.",
                    affectionDelta = 7,
                    crushExpression = Expression.HAPPY,
                    memoryTitleVi = "Lời hẹn ngắm hoàng hôn ngày $day"
                ),
                DialogueOption(
                    labelVi = "Chia sẻ câu chuyện trồng cây ở nông trại",
                    responseVi = "Nghe cậu kể say sưa dễ thương ghê! Mỗi hạt giống cậu gieo đều mang theo niềm vui mà.",
                    affectionDelta = 5,
                    crushExpression = Expression.SHY
                ),
                DialogueOption(
                    labelVi = "Hỏi xem $crushName thích món quà gì nhất",
                    responseVi = "Bí mật nha... nhưng mình cực kỳ thích Dâu tây ngọt, Hướng dương và hoa Oải hương tím đó!",
                    affectionDelta = 4,
                    crushExpression = Expression.HAPPY
                )
            )
            RelationshipTier.CRUSH, RelationshipTier.LOVE -> listOf(
                DialogueOption(
                    labelVi = "Nắm tay $crushName ngắm cảnh khu vườn",
                    responseVi = "Cậu... làm mình bất ngờ quá! Tay cậu ấm thật đấy... Đừng buông ra vội nhé.",
                    affectionDelta = 9,
                    crushExpression = Expression.LOVE,
                    memoryTitleVi = "Cái nắm tay dịu dàng tại ${zone.nameVi}"
                ),
                DialogueOption(
                    labelVi = "Hứa sẽ cùng nhau xây dựng nông trại mơ ước",
                    responseVi = "Mình tin ở tương lai của tụi mình. Khu vườn có cậu, có hoa và có tiếng cười mỗi ngày!",
                    affectionDelta = 8,
                    crushExpression = Expression.HAPPY,
                    memoryTitleVi = "Lời hứa chung đôi ngày $day"
                ),
                DialogueOption(
                    labelVi = "Trêu nhẹ khiến $crushName đỏ mặt",
                    responseVi = "Này! Đừng có nhìn mình chăm chú như thế chứ, ngại chết đi được!",
                    affectionDelta = 6,
                    crushExpression = Expression.SHY
                )
            )
        }

        val initialExp = when (tier) {
            RelationshipTier.LOVE, RelationshipTier.CRUSH -> Expression.SHY
            RelationshipTier.FRIEND, RelationshipTier.CLOSE_FRIEND -> Expression.HAPPY
            else -> Expression.NEUTRAL
        }

        return ActiveDialogue(
            speakerName = crushName,
            textVi = fullText,
            expression = initialExp,
            options = options
        )
    }

    fun evaluateGiftReaction(crushName: String, item: InventoryItem): Triple<String, Int, Expression> {
        val crop = item.cropType
        return when {
            crop?.crushLikedLevel == 2 -> Triple(
                "Ôi! ${item.nameVi} sao?! Đây là thứ mình thích nhất luôn đó! Cảm ơn cậu nhiều thật nhiều nha!",
                14,
                Expression.LOVE
            )
            item.category == ItemCategory.GIFT_ITEM || crop?.crushLikedLevel == 1 -> Triple(
                "Quà dễ thương quá, cảm ơn cậu đã nhớ tới mình! Mình sẽ trân trọng món quà này.",
                9,
                Expression.HAPPY
            )
            else -> Triple(
                "Cảm ơn cậu vì ${item.nameVi} nhé, nhận được quà từ cậu làm mình vui lắm.",
                5,
                Expression.HAPPY
            )
        }
    }

    fun buildDateEvent(
        dateId: String,
        crushName: String,
        playerName: String,
        day: Int
    ): ActiveDialogue {
        return when (dateId) {
            "date_sunset_lake" -> ActiveDialogue(
                speakerName = crushName,
                textVi = "Ánh hoàng hôn cam hồng nhuộm kín mặt hồ nước, từng gợn sóng lấp lánh như rắc vàng.\n\n$crushName ngồi sát bên bạn trên chiếc ghế gỗ: \"Ở bên $playerName lúc mặt trời lặn thế này, mọi âu lo đều tan biến hết.\"",
                expression = Expression.LOVE,
                isDateEvent = true,
                options = listOf(
                    DialogueOption(
                        labelVi = "\"Cảnh đẹp nhất là có cậu ở đây\"",
                        responseVi = "$crushName khẽ mỉm cười, má ửng hồng dưới ánh chiều tà: \"Mình cũng vậy... mong ngày nào cũng được ngắm hoàng hôn cùng cậu.\"",
                        affectionDelta = 15,
                        crushExpression = Expression.LOVE,
                        memoryTitleVi = "Buổi hẹn hoàng hôn bên Hồ Nước (Ngày $day)"
                    ),
                    DialogueOption(
                        labelVi = "Cùng lắng nghe tiếng nước vỗ nhẹ",
                        responseVi = "Hai người tựa vai nhau yên lặng lắng nghe tiếng gió và mặt hồ rì rào đến tận khi sao đầu tiên xuất hiện.",
                        affectionDelta = 12,
                        crushExpression = Expression.HAPPY,
                        memoryTitleVi = "Chiều bình yên bên Hồ Nước (Ngày $day)"
                    )
                )
            )
            "date_meadow_picnic" -> ActiveDialogue(
                speakerName = crushName,
                textVi = "Giữa Đồng hoa rực rỡ sắc cúc và tulip, hương thơm dịu nhẹ theo làn gió mát.\n\n$crushName trải tấm khăn caro nhỏ: \"Mình chuẩn bị trà hoa và bánh ngọt cho buổi dã ngoại của riêng hai đứa mình nè!\"",
                expression = Expression.HAPPY,
                isDateEvent = true,
                options = listOf(
                    DialogueOption(
                        labelVi = "Cài một bông hoa nhỏ lên tóc $crushName",
                        responseVi = "\"Ơ... trông có hợp không?\" $crushName mỉm cười rạng rỡ khiến cả đồng hoa như sáng bừng lên.",
                        affectionDelta = 14,
                        crushExpression = Expression.SHY,
                        memoryTitleVi = "Dã ngoại Đồng Hoa & Bông hoa nhỏ (Ngày $day)"
                    ),
                    DialogueOption(
                        labelVi = "Khen bánh và trà $crushName làm thật ngon",
                        responseVi = "\"Lần sau mình làm thêm nhiều món nữa cho cậu mang theo ra nông trại nhé!\"",
                        affectionDelta = 11,
                        crushExpression = Expression.HAPPY,
                        memoryTitleVi = "Tiệc trà Đồng Hoa (Ngày $day)"
                    )
                )
            )
            else -> ActiveDialogue(
                speakerName = crushName,
                textVi = "Tại Khu vực bí mật giữa rừng cây, viên Đá Ước Nguyện phát sáng dịu dàng bên hàng ngàn đom đóm lấp lánh.\n\n$crushName thì thầm: \"Người ta nói nếu hai người cùng ước dưới ánh đom đóm, điều ước sẽ thành sự thật.\"",
                expression = Expression.LOVE,
                isDateEvent = true,
                options = listOf(
                    DialogueOption(
                        labelVi = "Ước hai đứa mãi ở bên khu vườn này",
                        responseVi = "Ánh sáng xanh ngọc từ viên đá khẽ lóe lên. $crushName nắm chặt tay bạn: \"Điều ước của mình... cũng giống hệt cậu.\"",
                        affectionDelta = 18,
                        crushExpression = Expression.LOVE,
                        memoryTitleVi = "Lời ước dưới Đom Đóm Bí Mật (Ngày $day)"
                    )
                )
            )
        }
    }
}
