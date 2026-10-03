package com.example

import com.example.data.model.BlockType
import com.example.data.model.PostBlock
import com.example.data.model.PostBlockJsonHelper
import com.example.data.preferences.AppPreferences
import com.example.network.BloggerHtmlGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PostBlockTest {

    @Test
    fun testPostBlockSerialization() {
        val blocks = listOf(
            PostBlock(
                type = BlockType.LINK_BUTTON,
                text = "Download App",
                buttonUrl = "https://blogger.com",
                buttonBgColor = 0xFFFF5722L,
                buttonCornerRadius = 16,
                buttonStrokeWidth = 2,
                orderIndex = 0
            ),
            PostBlock(
                type = BlockType.COPY_BOX,
                copyPromptText = "Ultra realistic cinematic portrait",
                copyButtonLabel = "Copy Prompt",
                orderIndex = 1
            )
        )

        val json = PostBlockJsonHelper.toJson(blocks)
        assertNotNull(json)
        assertTrue(json.contains("Download App"))
        assertTrue(json.contains("Ultra realistic cinematic portrait"))

        val restored = PostBlockJsonHelper.fromJson(json)
        assertEquals(2, restored.size)
        assertEquals("Download App", restored[0].text)
        assertEquals(BlockType.LINK_BUTTON, restored[0].type)
        assertEquals(16, restored[0].buttonCornerRadius)
        assertEquals(BlockType.COPY_BOX, restored[1].type)
        assertEquals("Ultra realistic cinematic portrait", restored[1].copyPromptText)
    }

    @Test
    fun testBloggerHtmlGenerator() {
        val blocks = listOf(
            PostBlock(
                type = BlockType.LINK_BUTTON,
                text = "Click to Explore",
                buttonUrl = "https://google.com",
                orderIndex = 0
            ),
            PostBlock(
                type = BlockType.COPY_BOX,
                copyPromptText = "Cyberpunk street in rain, 4k",
                orderIndex = 1
            )
        )

        val html = BloggerHtmlGenerator.generateHtml(
            title = "Test Blogger Post",
            description = "Welcome to Blogger Studio",
            heroImageUrl = "https://example.com/banner.jpg",
            heroImageRotation = 0f,
            blocks = blocks,
            isDarkMode = false
        )

        assertTrue(html.contains("Test Blogger Post"))
        assertTrue(html.contains("Welcome to Blogger Studio"))
        assertTrue(html.contains("Click to Explore"))
        assertTrue(html.contains("Cyberpunk street in rain, 4k"))
        assertTrue(html.contains("navigator.clipboard.writeText"))
        assertTrue(html.contains("aspect-ratio: 1 / 1"))
    }

    @Test
    fun testAdminDefaultCredentials() {
        assertEquals("officialankityadav1", AppPreferences.DEFAULT_ADMIN_USER)
        assertEquals("Ankit13@", AppPreferences.DEFAULT_ADMIN_PASS)
    }
}
