package com.example.data.model

import java.util.UUID

enum class BlockType {
    BANNER_PHOTO,
    TEXT_PARAGRAPH,
    LINK_BUTTON,
    PHOTO,
    COPY_BOX,
    ACTION_BUTTON
}

data class PostBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType = BlockType.TEXT_PARAGRAPH,
    var orderIndex: Int = 0,
    var text: String = "",
    var imageUrl: String = "",
    var imageRotation: Float = 0f,
    var imageScale: Float = 1f,
    var imageCropAspect: String = "16:9",
    var buttonUrl: String = "",
    var buttonBgColor: Long = 0xFFFF5722L, // Blogger Orange
    var buttonTextColor: Long = 0xFFFFFFFFL,
    var buttonStrokeColor: Long = 0xFFE64A19L,
    var buttonStrokeWidth: Int = 0,
    var buttonCornerRadius: Int = 12,
    var buttonShadowElevation: Int = 4,
    var buttonSize: String = "FULL", // "SMALL", "MEDIUM", "LARGE", "FULL"
    var copyPromptText: String = "",
    var copyButtonLabel: String = "Copy Prompt"
) {
    fun copyBlock(): PostBlock {
        return this.copy(id = UUID.randomUUID().toString())
    }
}
