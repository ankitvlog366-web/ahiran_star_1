package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BlockType
import com.example.data.model.PostBlock
import com.example.network.BloggerHtmlGenerator

@Composable
fun LivePreviewDialog(
    title: String,
    description: String,
    heroImageUrl: String?,
    heroImageRotation: Float = 0f,
    heroImageScale: Float = 1f,
    blocks: List<PostBlock>,
    isDarkModeDefault: Boolean = false,
    onDismiss: () -> Unit,
    onPublishClicked: () -> Unit
) {
    val context = LocalContext.current
    var isDarkPreview by remember { mutableStateOf(isDarkModeDefault) }

    val previewBg = if (isDarkPreview) Color(0xFF121212) else Color(0xFFFFFFFF)
    val previewText = if (isDarkPreview) Color(0xFFEEEEEE) else Color(0xFF212121)
    val cardSurface = if (isDarkPreview) Color(0xFF1E1E1E) else Color(0xFFF9F9F9)
    val borderColor = if (isDarkPreview) Color(0xFF333333) else Color(0xFFE0E0E0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("live_preview_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Blogger Live Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDarkPreview) "Viewing Dark Mode" else "Viewing Light Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Theme Toggle for preview
                        IconButton(onClick = { isDarkPreview = !isDarkPreview }) {
                            Icon(
                                if (isDarkPreview) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Preview Theme",
                                tint = if (isDarkPreview) Color(0xFFFFB300) else Color(0xFF424242)
                            )
                        }

                        // Copy Generated HTML
                        IconButton(onClick = {
                            val html = BloggerHtmlGenerator.generateHtml(
                                title = title,
                                description = description,
                                heroImageUrl = heroImageUrl,
                                heroImageRotation = heroImageRotation,
                                blocks = blocks,
                                isDarkMode = isDarkPreview
                            )
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Blogger HTML", html))
                            Toast.makeText(context, "Blogger HTML copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy HTML")
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider()

                // Scrollable Live Article Preview Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(previewBg)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title
                        Text(
                            text = title.ifBlank { "Sample Post Title" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = previewText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hero Banner Image
                        if (!heroImageUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = heroImageUrl,
                                    contentDescription = "Post Hero Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(heroImageScale)
                                        .rotate(heroImageRotation)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Main Description
                        if (description.isNotBlank()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp,
                                color = previewText
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Reordered Blocks
                        val sortedBlocks = blocks.sortedBy { it.orderIndex }
                        sortedBlocks.forEach { block ->
                            when (block.type) {
                                BlockType.LINK_BUTTON -> {
                                    val btnBg = Color(block.buttonBgColor)
                                    val btnText = Color(block.buttonTextColor)
                                    val btnStroke = Color(block.buttonStrokeColor)

                                    val widthModifier = when (block.buttonSize) {
                                        "SMALL" -> Modifier.wrapContentWidth()
                                        "MEDIUM" -> Modifier.widthIn(min = 160.dp)
                                        "LARGE" -> Modifier.widthIn(min = 220.dp)
                                        else -> Modifier.fillMaxWidth()
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .then(widthModifier)
                                                .shadow(block.buttonShadowElevation.dp, shape = RoundedCornerShape(block.buttonCornerRadius.dp))
                                                .clip(RoundedCornerShape(block.buttonCornerRadius.dp))
                                                .background(btnBg)
                                                .then(
                                                    if (block.buttonStrokeWidth > 0) Modifier.border(
                                                        block.buttonStrokeWidth.dp,
                                                        btnStroke,
                                                        RoundedCornerShape(block.buttonCornerRadius.dp)
                                                    ) else Modifier
                                                )
                                                .padding(
                                                    horizontal = when (block.buttonSize) {
                                                        "SMALL" -> 16.dp
                                                        "LARGE" -> 32.dp
                                                        else -> 24.dp
                                                    },
                                                    vertical = when (block.buttonSize) {
                                                        "SMALL" -> 10.dp
                                                        "LARGE" -> 18.dp
                                                        else -> 14.dp
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = block.text.ifBlank { "Click Here" },
                                                color = btnText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = when (block.buttonSize) {
                                                    "SMALL" -> 14.sp
                                                    "LARGE" -> 18.sp
                                                    else -> 16.sp
                                                },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                BlockType.PHOTO -> {
                                    if (block.imageUrl.isNotBlank()) {
                                        val aspect = when (block.imageCropAspect) {
                                            "1:1" -> 1f
                                            "4:3" -> 4f / 3f
                                            else -> 16f / 9f
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                                .aspectRatio(aspect)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = block.imageUrl,
                                                contentDescription = "Block photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .scale(block.imageScale)
                                                    .rotate(block.imageRotation)
                                            )
                                        }
                                        if (block.text.isNotBlank()) {
                                            Text(
                                                text = block.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                BlockType.COPY_BOX -> {
                                    PromptCopyBoxView(
                                        promptText = block.copyPromptText.ifBlank { block.text },
                                        buttonLabel = block.copyButtonLabel,
                                        isDarkMode = isDarkPreview,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }

                                BlockType.ACTION_BUTTON -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(14.dp)),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkPreview) Color(0xFF261D15) else Color(0xFFFFF9E6)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = block.copyPromptText.ifBlank { block.text },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = previewText
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Action Prompt", block.copyPromptText))
                                                    Toast.makeText(context, "Prompt copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(block.copyButtonLabel.ifBlank { "Copy Prompt" }, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                BlockType.TEXT_PARAGRAPH, BlockType.BANNER_PHOTO -> {
                                    if (block.text.isNotBlank()) {
                                        Text(
                                            text = block.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 24.sp,
                                            color = previewText,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }

                // Bottom Action Bar in Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Back to Editor")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onPublishClicked()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish to Blogger Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
