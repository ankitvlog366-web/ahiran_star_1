package com.example.network

import com.example.data.model.BlockType
import com.example.data.model.PostBlock

object BloggerHtmlGenerator {

    /**
     * Converts the post title, description, and list of modular blocks into rich,
     * responsive, beautiful HTML compatible with Blogger Web.
     */
    fun generateHtml(
        title: String,
        description: String,
        heroImageUrl: String?,
        heroImageRotation: Float = 0f,
        blocks: List<PostBlock>,
        isDarkMode: Boolean = false
    ): String {
        val textColor = if (isDarkMode) "#FFFFFF" else "#212121"
        val bgColor = if (isDarkMode) "#1A1A1A" else "#FAFAFA"
        val cardBg = if (isDarkMode) "#2D2D2D" else "#FFFFFF"
        val borderColor = if (isDarkMode) "#424242" else "#E0E0E0"

        val sb = StringBuilder()

        sb.append("<!-- Blogger Studio Generated Post -->\n")
        sb.append("<div class=\"blogger-studio-container\" style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 720px; margin: 0 auto; line-height: 1.6; color: $textColor;\">\n")

        // Hero Image
        if (!heroImageUrl.isNullOrBlank()) {
            val transform = if (heroImageRotation != 0f) "transform: rotate(${heroImageRotation}deg);" else ""
            sb.append("""
                <div style="text-align: center; margin-bottom: 24px;">
                    <img src="$heroImageUrl" alt="$title" style="max-width: 100%; height: auto; border-radius: 14px; box-shadow: 0 4px 16px rgba(0,0,0,0.12); $transform display: inline-block;" />
                </div>
            """.trimIndent())
            sb.append("\n")
        }

        // Main Description Paragraph
        if (description.isNotBlank()) {
            val formattedDesc = description.replace("\n", "<br/>")
            sb.append("""
                <div style="font-size: 17px; margin-bottom: 24px; padding: 4px 0;">
                    <p style="margin: 0; line-height: 1.7;">$formattedDesc</p>
                </div>
            """.trimIndent())
            sb.append("\n")
        }

        // Modular Blocks in Exact User Defined Order
        val sortedBlocks = blocks.sortedBy { it.orderIndex }
        for ((index, block) in sortedBlocks.withIndex()) {
            when (block.type) {
                BlockType.LINK_BUTTON -> {
                    val hexBg = String.format("#%06X", (0xFFFFFFL and block.buttonBgColor))
                    val hexText = String.format("#%06X", (0xFFFFFFL and block.buttonTextColor))
                    val hexStroke = String.format("#%06X", (0xFFFFFFL and block.buttonStrokeColor))
                    val strokeCss = if (block.buttonStrokeWidth > 0) "border: ${block.buttonStrokeWidth}px solid $hexStroke;" else "border: none;"
                    val shadowCss = if (block.buttonShadowElevation > 0) "box-shadow: 0 ${block.buttonShadowElevation}px ${block.buttonShadowElevation * 2}px rgba(0,0,0,0.2);" else "box-shadow: none;"
                    val radiusCss = "border-radius: ${block.buttonCornerRadius}px;"

                    val (widthCss, paddingCss, fontCss) = when (block.buttonSize) {
                        "SMALL" -> Triple("width: auto;", "padding: 8px 16px;", "font-size: 14px;")
                        "MEDIUM" -> Triple("width: auto;", "padding: 12px 24px;", "font-size: 16px;")
                        "LARGE" -> Triple("width: auto;", "padding: 16px 32px;", "font-size: 18px;")
                        else -> Triple("display: block; width: 100%; box-sizing: border-box;", "padding: 14px 20px;", "font-size: 16px;")
                    }

                    sb.append("""
                        <div style="text-align: center; margin: 18px 0;">
                            <a href="${block.buttonUrl}" target="_blank" rel="noopener noreferrer" style="display: inline-block; background-color: $hexBg; color: $hexText; $strokeCss $shadowCss $radiusCss $paddingCss $fontCss text-decoration: none; font-weight: 600; text-align: center; transition: transform 0.2s, box-shadow 0.2s;">
                                ${block.text.ifBlank { "Click Here" }}
                            </a>
                        </div>
                    """.trimIndent())
                    sb.append("\n")
                }

                BlockType.PHOTO -> {
                    if (block.imageUrl.isNotBlank()) {
                        val transform = if (block.imageRotation != 0f) "transform: rotate(${block.imageRotation}deg);" else ""
                        sb.append("""
                            <div style="text-align: center; margin: 20px 0;">
                                <img src="${block.imageUrl}" style="max-width: 100%; height: auto; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); $transform display: inline-block;" />
                                ${if (block.text.isNotBlank()) "<p style=\"font-size: 13px; color: #888; margin-top: 6px;\">${block.text}</p>" else ""}
                            </div>
                        """.trimIndent())
                        sb.append("\n")
                    }
                }

                BlockType.COPY_BOX -> {
                    val promptText = block.copyPromptText.ifBlank { block.text }
                    val boxId = "copy_box_$index"
                    val escapedPrompt = promptText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "")

                    sb.append("""
                        <div style="margin: 24px 0; border: 1.5px solid $borderColor; border-radius: 16px; background-color: $cardBg; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06);">
                            <!-- Top Header Bar with Copy Button -->
                            <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background-color: ${if (isDarkMode) "#383838" else "#F0F0F0"}; border-bottom: 1px solid $borderColor;">
                                <span style="font-weight: 600; font-size: 14px; color: $textColor;">Prompt / Text</span>
                                <button id="btn_$boxId" onclick="(function(){
                                    navigator.clipboard.writeText('$escapedPrompt');
                                    var b = document.getElementById('btn_$boxId');
                                    b.innerText = 'Copied!';
                                    b.style.backgroundColor = '#4CAF50';
                                    setTimeout(function(){ b.innerText = '${block.copyButtonLabel}'; b.style.backgroundColor = '#FF5722'; }, 2000);
                                })()" style="background-color: #FF5722; color: #ffffff; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 13px; transition: background 0.2s;">
                                    ${block.copyButtonLabel}
                                </button>
                            </div>
                            <!-- 1:1 Aspect Ratio Scrollable Content Container -->
                            <div style="aspect-ratio: 1 / 1; max-height: 400px; overflow-y: auto; padding: 16px; box-sizing: border-box; background-color: $cardBg;">
                                <pre style="margin: 0; white-space: pre-wrap; word-break: break-word; font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 14px; line-height: 1.6; color: $textColor;">$promptText</pre>
                            </div>
                        </div>
                    """.trimIndent())
                    sb.append("\n")
                }

                BlockType.ACTION_BUTTON -> {
                    val promptText = block.copyPromptText.ifBlank { block.text }
                    val actionBtnId = "action_btn_$index"
                    val escapedPrompt = promptText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "")

                    sb.append("""
                        <div style="margin: 20px 0; padding: 16px; border: 1.5px dashed #FF9800; border-radius: 14px; background-color: ${if (isDarkMode) "#2D261E" else "#FFF8E1"};">
                            <p style="margin: 0 0 10px 0; font-size: 14px; font-weight: 500; color: $textColor;">$promptText</p>
                            <button id="$actionBtnId" onclick="(function(){
                                navigator.clipboard.writeText('$escapedPrompt');
                                var b = document.getElementById('$actionBtnId');
                                b.innerText = 'Prompt Copied!';
                                setTimeout(function(){ b.innerText = '${block.copyButtonLabel.ifBlank { "Copy Prompt" }}'; }, 2000);
                            })()" style="background-color: #E65100; color: #ffffff; border: none; padding: 10px 20px; border-radius: 10px; font-weight: bold; cursor: pointer; font-size: 14px; width: 100%;">
                                ${block.copyButtonLabel.ifBlank { "Copy Prompt" }}
                            </button>
                        </div>
                    """.trimIndent())
                    sb.append("\n")
                }

                BlockType.TEXT_PARAGRAPH, BlockType.BANNER_PHOTO -> {
                    if (block.text.isNotBlank()) {
                        val formatted = block.text.replace("\n", "<br/>")
                        sb.append("""
                            <div style="font-size: 16px; margin: 16px 0; color: $textColor;">
                                <p style="margin: 0; line-height: 1.7;">$formatted</p>
                            </div>
                        """.trimIndent())
                        sb.append("\n")
                    }
                }
            }
        }

        sb.append("</div>\n")
        return sb.toString()
    }
}
