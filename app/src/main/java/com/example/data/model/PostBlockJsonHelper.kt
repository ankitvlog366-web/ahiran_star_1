package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

object PostBlockJsonHelper {

    fun toJson(blocks: List<PostBlock>): String {
        val array = JSONArray()
        for (b in blocks) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("type", b.type.name)
            obj.put("orderIndex", b.orderIndex)
            obj.put("text", b.text)
            obj.put("imageUrl", b.imageUrl)
            obj.put("imageRotation", b.imageRotation.toDouble())
            obj.put("imageScale", b.imageScale.toDouble())
            obj.put("imageCropAspect", b.imageCropAspect)
            obj.put("buttonUrl", b.buttonUrl)
            obj.put("buttonBgColor", b.buttonBgColor)
            obj.put("buttonTextColor", b.buttonTextColor)
            obj.put("buttonStrokeColor", b.buttonStrokeColor)
            obj.put("buttonStrokeWidth", b.buttonStrokeWidth)
            obj.put("buttonCornerRadius", b.buttonCornerRadius)
            obj.put("buttonShadowElevation", b.buttonShadowElevation)
            obj.put("buttonSize", b.buttonSize)
            obj.put("copyPromptText", b.copyPromptText)
            obj.put("copyButtonLabel", b.copyButtonLabel)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(jsonStr: String?): List<PostBlock> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<PostBlock>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", BlockType.TEXT_PARAGRAPH.name)
                val type = try {
                    BlockType.valueOf(typeName)
                } catch (e: Exception) {
                    BlockType.TEXT_PARAGRAPH
                }
                val block = PostBlock(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    type = type,
                    orderIndex = obj.optInt("orderIndex", i),
                    text = obj.optString("text", ""),
                    imageUrl = obj.optString("imageUrl", ""),
                    imageRotation = obj.optDouble("imageRotation", 0.0).toFloat(),
                    imageScale = obj.optDouble("imageScale", 1.0).toFloat(),
                    imageCropAspect = obj.optString("imageCropAspect", "16:9"),
                    buttonUrl = obj.optString("buttonUrl", ""),
                    buttonBgColor = obj.optLong("buttonBgColor", 0xFFFF5722L),
                    buttonTextColor = obj.optLong("buttonTextColor", 0xFFFFFFFFL),
                    buttonStrokeColor = obj.optLong("buttonStrokeColor", 0xFFE64A19L),
                    buttonStrokeWidth = obj.optInt("buttonStrokeWidth", 0),
                    buttonCornerRadius = obj.optInt("buttonCornerRadius", 12),
                    buttonShadowElevation = obj.optInt("buttonShadowElevation", 4),
                    buttonSize = obj.optString("buttonSize", "FULL"),
                    copyPromptText = obj.optString("copyPromptText", ""),
                    copyButtonLabel = obj.optString("copyButtonLabel", "Copy Prompt")
                )
                list.add(block)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
