package com.example.network

import com.example.data.model.BlockType
import com.example.data.model.PostBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedArticle(
    val title: String,
    val description: String,
    val heroImageUrl: String?,
    val blocks: List<PostBlock>
)

class ArticleExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun extractArticle(url: String): ExtractedArticle = withContext(Dispatchers.IO) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            // Check if JSON response
            if (responseBody.trim().startsWith("{")) {
                try {
                    val json = JSONObject(responseBody)
                    val title = json.optString("title", json.optString("name", "Extracted Article"))
                    val desc = json.optString("description", json.optString("content", json.optString("body", "")))
                    val image = json.optString("image", json.optString("heroImage", json.optString("thumbnail", "")))

                    val blocks = mutableListOf<PostBlock>()
                    if (desc.isNotBlank()) {
                        blocks.add(
                            PostBlock(
                                type = BlockType.TEXT_PARAGRAPH,
                                text = desc,
                                orderIndex = 0
                            )
                        )
                    }
                    return@withContext ExtractedArticle(
                        title = title,
                        description = desc.take(200),
                        heroImageUrl = image.ifBlank { null },
                        blocks = blocks
                    )
                } catch (e: Exception) {
                    // Fall back to HTML parsing
                }
            }

            // HTML Parsing via regex
            val title = extractTitle(responseBody)
            val heroImage = extractHeroImage(responseBody, cleanUrl)
            val paragraphs = extractParagraphs(responseBody)

            val blocks = mutableListOf<PostBlock>()
            var order = 0

            // Combine first couple paragraphs into main description
            val mainDesc = if (paragraphs.isNotEmpty()) {
                paragraphs.take(2).joinToString("\n\n")
            } else {
                "Article extracted from $cleanUrl"
            }

            // Remaining paragraphs as blocks
            for (p in paragraphs.drop(2).take(8)) {
                if (p.length > 20) {
                    blocks.add(
                        PostBlock(
                            type = BlockType.TEXT_PARAGRAPH,
                            text = p,
                            orderIndex = order++
                        )
                    )
                }
            }

            // Add a source link button block
            blocks.add(
                PostBlock(
                    type = BlockType.LINK_BUTTON,
                    text = "Visit Original Source",
                    buttonUrl = cleanUrl,
                    buttonBgColor = 0xFFFF5722L,
                    buttonTextColor = 0xFFFFFFFFL,
                    buttonCornerRadius = 12,
                    orderIndex = order++
                )
            )

            ExtractedArticle(
                title = title.ifBlank { "Extracted Post from Web" },
                description = mainDesc,
                heroImageUrl = heroImage,
                blocks = blocks
            )
        } catch (e: Exception) {
            // Return fallback extracted state
            ExtractedArticle(
                title = "Imported Web Article",
                description = "Content extracted from $cleanUrl. Network request status: ${e.localizedMessage ?: "Done"}",
                heroImageUrl = null,
                blocks = listOf(
                    PostBlock(
                        type = BlockType.LINK_BUTTON,
                        text = "Open Source Link",
                        buttonUrl = cleanUrl,
                        orderIndex = 0
                    )
                )
            )
        }
    }

    private fun extractTitle(html: String): String {
        // og:title
        val ogMatch = Pattern.compile("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogMatch.find()) return cleanHtmlText(ogMatch.group(1) ?: "")

        // <title>
        val titleMatch = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html)
        if (titleMatch.find()) return cleanHtmlText(titleMatch.group(1) ?: "")

        // <h1>
        val h1Match = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE).matcher(html)
        if (h1Match.find()) return cleanHtmlText(h1Match.group(1) ?: "")

        return ""
    }

    private fun extractHeroImage(html: String, baseUrl: String): String? {
        val ogImageMatch = Pattern.compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogImageMatch.find()) {
            val img = ogImageMatch.group(1) ?: ""
            if (img.startsWith("http")) return img
        }

        val imgMatch = Pattern.compile("<img[^>]+src=[\"'](https?://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (imgMatch.find()) {
            return imgMatch.group(1)
        }
        return null
    }

    private fun extractParagraphs(html: String): List<String> {
        val paragraphs = mutableListOf<String>()
        val pMatcher = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        while (pMatcher.find() && paragraphs.size < 15) {
            val text = cleanHtmlText(pMatcher.group(1) ?: "")
            if (text.length > 25 && !text.contains("cookies", ignoreCase = true) && !text.contains("privacy policy", ignoreCase = true)) {
                paragraphs.add(text)
            }
        }
        return paragraphs
    }

    private fun cleanHtmlText(raw: String): String {
        return raw.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
