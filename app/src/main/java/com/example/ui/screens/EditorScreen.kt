package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BlogPostEntity
import com.example.data.model.BlockType
import com.example.data.model.PostBlock
import com.example.data.model.PostBlockJsonHelper
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BlogRepository
import com.example.network.AiGenerateResult
import com.example.network.BloggerHtmlGenerator
import com.example.network.BloggerPublisher
import com.example.network.GeminiService
import com.example.network.PublishResult
import com.example.ui.components.ButtonStylerSheet
import com.example.ui.components.ImageEditorDialog
import com.example.ui.components.LivePreviewDialog
import com.example.ui.components.PromptCopyBoxView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    appPreferences: AppPreferences,
    blogRepository: BlogRepository,
    editingPost: BlogPostEntity? = null,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bloggerPublisher = remember { BloggerPublisher() }
    val geminiService = remember { GeminiService() }

    // Theme state inside editor
    var isDarkMode by remember { mutableStateOf(appPreferences.isDarkMode) }

    // Post content states
    var postId by remember { mutableStateOf(editingPost?.id ?: 0L) }
    var bloggerPostId by remember { mutableStateOf(editingPost?.bloggerPostId) }
    var title by remember { mutableStateOf(editingPost?.title ?: "") }
    var description by remember { mutableStateOf(editingPost?.description ?: "") }
    var heroImageUrl by remember { mutableStateOf(editingPost?.heroImageUrl) }
    var heroRotation by remember { mutableFloatStateOf(editingPost?.heroImageRotation ?: 0f) }
    var heroScale by remember { mutableFloatStateOf(editingPost?.heroImageScale ?: 1f) }
    var heroAspect by remember { mutableStateOf(editingPost?.heroImageCropAspect ?: "16:9") }

    // Content Blocks with explicit List<PostBlock> type
    val initialBlocks: List<PostBlock> = remember(editingPost) {
        if (editingPost != null && editingPost.blocksJson.isNotBlank()) {
            PostBlockJsonHelper.fromJson(editingPost.blocksJson)
        } else {
            emptyList()
        }
    }
    var blocks by remember { mutableStateOf<List<PostBlock>>(initialBlocks) }

    // Dialog & Sheet States
    var showLivePreview by remember { mutableStateOf(false) }
    var showAddMoreSheet by remember { mutableStateOf(false) }
    var stylingBlock by remember { mutableStateOf<PostBlock?>(null) }
    var editingImageBlockIndex by remember { mutableStateOf<Int?>(null) }
    var editingHeroImage by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var aiWordLimit by remember { mutableIntStateOf(300) }
    var aiTopic by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishResultDialog by remember { mutableStateOf<PublishResult?>(null) }

    // Preview specific block dialog
    var previewingSingleBlock by remember { mutableStateOf<PostBlock?>(null) }

    // Photo pickers
    val heroPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            heroImageUrl = uri.toString()
            editingHeroImage = true
        }
    }

    var blockPhotoTargetIndex by remember { mutableStateOf<Int?>(null) }
    val blockPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetIdx = blockPhotoTargetIndex
        if (uri != null && targetIdx != null) {
            if (targetIdx in blocks.indices) {
                val updated = blocks.toMutableList()
                updated[targetIdx] = updated[targetIdx].copy(imageUrl = uri.toString())
                blocks = updated
                editingImageBlockIndex = targetIdx
            }
        }
    }

    val editorBg = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)
    val editorCardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val editorTextColor = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)
    val editorSecondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF616161)

    // Reordering helper functions
    val moveBlockUp = { index: Int ->
        if (index > 0) {
            val list = blocks.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            list.forEachIndexed { i, b -> b.orderIndex = i }
            blocks = list
        }
    }

    val moveBlockDown = { index: Int ->
        if (index < blocks.size - 1) {
            val list = blocks.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            list.forEachIndexed { i, b -> b.orderIndex = i }
            blocks = list
        }
    }

    val deleteBlock = { index: Int ->
        val list = blocks.toMutableList()
        list.removeAt(index)
        list.forEachIndexed { i, b -> b.orderIndex = i }
        blocks = list
    }

    // Publish / Save Logic
    val doPublish = {
        if (title.isBlank()) {
            Toast.makeText(context, "Please enter a post title before publishing.", Toast.LENGTH_SHORT).show()
        } else {
            isPublishing = true
            coroutineScope.launch {
                val html = BloggerHtmlGenerator.generateHtml(
                    title = title,
                    description = description,
                    heroImageUrl = heroImageUrl,
                    heroImageRotation = heroRotation,
                    blocks = blocks,
                    isDarkMode = isDarkMode
                )

                val token = appPreferences.bloggerAccessToken
                val bId = appPreferences.bloggerBlogId

                if (token.isBlank() || bId.isBlank()) {
                    val draftEntity = BlogPostEntity(
                        id = postId,
                        bloggerPostId = bloggerPostId,
                        title = title,
                        heroImageUrl = heroImageUrl,
                        heroImageRotation = heroRotation,
                        heroImageScale = heroScale,
                        heroImageCropAspect = heroAspect,
                        description = description,
                        blocksJson = PostBlockJsonHelper.toJson(blocks),
                        status = "DRAFT"
                    )
                    val newId = blogRepository.savePost(draftEntity)
                    postId = newId
                    isPublishing = false
                    publishResultDialog = PublishResult.Error(
                        "Post saved as Draft! Blogger credentials are not configured yet. Please open Settings to set your Blogger Token & Blog ID."
                    )
                } else {
                    val result = if (!bloggerPostId.isNullOrBlank()) {
                        bloggerPublisher.updatePost(token, bId, bloggerPostId!!, title, html)
                    } else {
                        bloggerPublisher.publishPost(token, bId, title, html)
                    }

                    when (result) {
                        is PublishResult.Success -> {
                            val savedEntity = BlogPostEntity(
                                id = postId,
                                bloggerPostId = result.postId,
                                bloggerUrl = result.url,
                                title = title,
                                heroImageUrl = heroImageUrl,
                                heroImageRotation = heroRotation,
                                heroImageScale = heroScale,
                                heroImageCropAspect = heroAspect,
                                description = description,
                                blocksJson = PostBlockJsonHelper.toJson(blocks),
                                status = "PUBLISHED",
                                publishedAt = System.currentTimeMillis()
                            )
                            val savedId = blogRepository.savePost(savedEntity)
                            postId = savedId
                            bloggerPostId = result.postId
                            publishResultDialog = result
                        }
                        is PublishResult.Error -> {
                            val failedEntity = BlogPostEntity(
                                id = postId,
                                bloggerPostId = bloggerPostId,
                                title = title,
                                heroImageUrl = heroImageUrl,
                                heroImageRotation = heroRotation,
                                heroImageScale = heroScale,
                                heroImageCropAspect = heroAspect,
                                description = description,
                                blocksJson = PostBlockJsonHelper.toJson(blocks),
                                status = "FAILED"
                            )
                            val savedId = blogRepository.savePost(failedEntity)
                            postId = savedId
                            publishResultDialog = result
                        }
                    }
                    isPublishing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (editingPost != null) "Edit Blogger Post" else "Create Blogger Post",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = editorTextColor
                        )
                        Text(
                            text = if (appPreferences.isBloggerConnected()) "Blog: ${appPreferences.bloggerBlogName}" else "Blog Not Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (appPreferences.isBloggerConnected()) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isDarkMode = !isDarkMode
                            appPreferences.isDarkMode = isDarkMode
                        },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Editor Theme",
                            tint = if (isDarkMode) Color(0xFFFFB300) else Color(0xFF424242)
                        )
                    }

                    IconButton(
                        onClick = { showLivePreview = true },
                        modifier = Modifier.testTag("top_preview_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Live Preview", tint = Color(0xFFFF5722))
                    }

                    Button(
                        onClick = { doPublish() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorCardBg
                )
            )
        },
        containerColor = editorBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // TOP BANNER / HERO IMAGE UPLOAD SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Featured / Hero Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = editorTextColor
                        )
                        Text(
                            text = "Add header banner photo with Crop, Rotate, and Zoom",
                            style = MaterialTheme.typography.bodySmall,
                            color = editorSecondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val heroImg = heroImageUrl
                        if (heroImg != null) {
                            val aspect = when (heroAspect) {
                                "1:1" -> 1f
                                "4:3" -> 4f / 3f
                                else -> 16f / 9f
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = heroImg,
                                    contentDescription = "Hero Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(heroScale)
                                        .rotate(heroRotation)
                                )

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { editingHeroImage = true },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.CropRotate, contentDescription = "Edit photo", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { heroImageUrl = null },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    heroPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color(0xFFFF5722))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Upload Photo from Gallery / Camera", fontWeight = FontWeight.SemiBold, color = editorTextColor)
                                }
                            }
                        }
                    }
                }
            }

            // POST TITLE FIELD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Post Title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = editorTextColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Write engaging post title here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("post_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // POST MAIN DESCRIPTION & AI GENERATOR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Description & Intro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = editorTextColor
                            )

                            Button(
                                onClick = { showAiGeneratorDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate with AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Write main post description here, or generate it automatically using AI...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .testTag("post_description_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // MODULAR CONTENT BLOCKS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modular Content Blocks (${blocks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = editorTextColor
                    )

                    Button(
                        onClick = { showAddMoreSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ More", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // MODULAR BLOCKS
            itemsIndexed(
                items = blocks,
                key = { _, item -> item.id }
            ) { index: Int, block: PostBlock ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_card_$index"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // LEFT SIDE: UP & DOWN REORDER ARROWS
                        Column(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = { moveBlockUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) Color(0xFFFF5722) else Color.LightGray
                                )
                            }

                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = editorSecondaryTextColor
                            )

                            IconButton(
                                onClick = { moveBlockDown(index) },
                                enabled = index < blocks.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < blocks.size - 1) Color(0xFFFF5722) else Color.LightGray
                                )
                            }
                        }

                        // CENTER CONTENT
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when (block.type) {
                                            BlockType.LINK_BUTTON -> Icons.Default.SmartButton
                                            BlockType.PHOTO -> Icons.Default.Image
                                            BlockType.COPY_BOX -> Icons.Default.ContentCopy
                                            BlockType.ACTION_BUTTON -> Icons.Default.TouchApp
                                            else -> Icons.Default.TextFields
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (block.type) {
                                            BlockType.LINK_BUTTON -> "Link Button"
                                            BlockType.PHOTO -> "Photo Block"
                                            BlockType.COPY_BOX -> "1:1 Copy Box"
                                            BlockType.ACTION_BUTTON -> "Copy-to-Create Button"
                                            else -> "Text Paragraph"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = editorTextColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalIconButton(
                                        onClick = { previewingSingleBlock = block },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Preview Item", modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { deleteBlock(index) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            when (block.type) {
                                BlockType.LINK_BUTTON -> {
                                    val btnBg = Color(block.buttonBgColor)
                                    val btnText = Color(block.buttonTextColor)
                                    val btnStroke = Color(block.buttonStrokeColor)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(block.buttonShadowElevation.dp, RoundedCornerShape(block.buttonCornerRadius.dp))
                                            .clip(RoundedCornerShape(block.buttonCornerRadius.dp))
                                            .background(btnBg)
                                            .then(
                                                if (block.buttonStrokeWidth > 0) Modifier.border(
                                                    block.buttonStrokeWidth.dp,
                                                    btnStroke,
                                                    RoundedCornerShape(block.buttonCornerRadius.dp)
                                                ) else Modifier
                                            )
                                            .clickable { stylingBlock = block }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (block.text.isNotBlank()) block.text else "Click Here",
                                            color = btnText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "URL: ${if (block.buttonUrl.isNotBlank()) block.buttonUrl else "(No URL)"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = editorSecondaryTextColor,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { stylingBlock = block }) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Customize Style", fontSize = 12.sp)
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
                                                .aspectRatio(aspect)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = block.imageUrl,
                                                contentDescription = "Photo Block",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .scale(block.imageScale)
                                                    .rotate(block.imageRotation)
                                            )

                                            IconButton(
                                                onClick = { editingImageBlockIndex = index },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                            ) {
                                                Icon(Icons.Default.CropRotate, contentDescription = "Edit photo", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                blockPhotoTargetIndex = index
                                                blockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pick Photo from Gallery/Camera")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = block.text,
                                        onValueChange = { newTxt: String ->
                                            val updated = blocks.toMutableList()
                                            updated[index] = updated[index].copy(text = newTxt)
                                            blocks = updated
                                        },
                                        placeholder = { Text("Photo caption / description (optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                BlockType.COPY_BOX -> {
                                    OutlinedTextField(
                                        value = block.copyPromptText,
                                        onValueChange = { newPrompt: String ->
                                            val updated = blocks.toMutableList()
                                            updated[index] = updated[index].copy(copyPromptText = newPrompt)
                                            blocks = updated
                                        },
                                        label = { Text("Prompt Text (shown in 1:1 scrollable box)") },
                                        placeholder = { Text("Paste prompt here...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 90.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    PromptCopyBoxView(
                                        promptText = block.copyPromptText,
                                        buttonLabel = block.copyButtonLabel,
                                        isDarkMode = isDarkMode,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                BlockType.ACTION_BUTTON -> {
                                    OutlinedTextField(
                                        value = block.copyPromptText,
                                        onValueChange = { newPrompt: String ->
                                            val updated = blocks.toMutableList()
                                            updated[index] = updated[index].copy(copyPromptText = newPrompt)
                                            blocks = updated
                                        },
                                        label = { Text("Prompt Snippet") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = block.copyButtonLabel,
                                        onValueChange = { newLbl: String ->
                                            val updated = blocks.toMutableList()
                                            updated[index] = updated[index].copy(copyButtonLabel = newLbl)
                                            blocks = updated
                                        },
                                        label = { Text("Button Text (e.g. Copy Prompt & Create)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                BlockType.TEXT_PARAGRAPH, BlockType.BANNER_PHOTO -> {
                                    OutlinedTextField(
                                        value = block.text,
                                        onValueChange = { newTxt: String ->
                                            val updated = blocks.toMutableList()
                                            updated[index] = updated[index].copy(text = newTxt)
                                            blocks = updated
                                        },
                                        placeholder = { Text("Enter section paragraph text...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 90.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY "PUBLISH TO BLOGGER" BOTTOM BUTTON
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { doPublish() },
                    enabled = !isPublishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("publish_bottom_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Publishing to Blogger...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.Publish, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!bloggerPostId.isNullOrBlank()) "Update Post on Blogger" else "Publish Post to Blogger Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showLivePreview = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Live Preview")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val draft = BlogPostEntity(
                                    id = postId,
                                    bloggerPostId = bloggerPostId,
                                    title = if (title.isNotBlank()) title else "Untitled Draft",
                                    heroImageUrl = heroImageUrl,
                                    heroImageRotation = heroRotation,
                                    heroImageScale = heroScale,
                                    heroImageCropAspect = heroAspect,
                                    description = description,
                                    blocksJson = PostBlockJsonHelper.toJson(blocks),
                                    status = "DRAFT"
                                )
                                postId = blogRepository.savePost(draft)
                                Toast.makeText(context, "Draft saved locally!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Local Draft")
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // "MORE" BOTTOM SHEET FOR ADDING CONTENT BLOCKS
    if (showAddMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddMoreSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add Modular Content Block",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose a block to add to your post. Unlimited blocks supported!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Add Link Button
                ListItem(
                    headlineContent = { Text("1. Add Link Button", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Custom styled button with stroke, shadow, colors & size") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF5722)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartButton, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable {
                        val newBlock = PostBlock(
                            type = BlockType.LINK_BUTTON,
                            text = "Visit Official Website",
                            buttonUrl = "https://",
                            buttonBgColor = 0xFFFF5722L,
                            buttonTextColor = 0xFFFFFFFFL,
                            buttonStrokeColor = 0xFFE64A19L,
                            buttonCornerRadius = 12,
                            buttonShadowElevation = 4,
                            orderIndex = blocks.size
                        )
                        blocks = blocks + newBlock
                        showAddMoreSheet = false
                        stylingBlock = newBlock
                    }
                )

                HorizontalDivider()

                // 2. Add Photo
                ListItem(
                    headlineContent = { Text("2. Add Photo", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Upload image from gallery/camera with crop, rotate & zoom") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable {
                        val newIdx = blocks.size
                        val newBlock = PostBlock(
                            type = BlockType.PHOTO,
                            orderIndex = newIdx
                        )
                        blocks = blocks + newBlock
                        showAddMoreSheet = false
                        blockPhotoTargetIndex = newIdx
                        blockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )

                HorizontalDivider()

                // 3. Copy Box (1:1 Aspect Ratio)
                ListItem(
                    headlineContent = { Text("3. Copy Box (1:1 Prompt Box)", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("1:1 aspect ratio scrollable box with top Copy Prompt button") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF9C27B0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable {
                        val newBlock = PostBlock(
                            type = BlockType.COPY_BOX,
                            copyPromptText = "Ultra realistic 8k portrait photography of a futuristic cyber hacker, neon volumetric lighting, shot on 85mm lens, sharp focus, octane render --ar 16:9 --v 6",
                            copyButtonLabel = "Copy Prompt",
                            orderIndex = blocks.size
                        )
                        blocks = blocks + newBlock
                        showAddMoreSheet = false
                    }
                )

                HorizontalDivider()

                // 4. Copy-to-Create Button
                ListItem(
                    headlineContent = { Text("4. Copy-to-Create Button", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Prompt snippet with instant action button to copy & create") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF9800)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable {
                        val newBlock = PostBlock(
                            type = BlockType.ACTION_BUTTON,
                            copyPromptText = "Paste your Midjourney / ChatGPT creative prompt here...",
                            copyButtonLabel = "Copy Prompt & Create",
                            orderIndex = blocks.size
                        )
                        blocks = blocks + newBlock
                        showAddMoreSheet = false
                    }
                )

                HorizontalDivider()

                // 5. Text Paragraph
                ListItem(
                    headlineContent = { Text("5. Text Paragraph", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Article body section paragraph") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF607D8B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable {
                        val newBlock = PostBlock(
                            type = BlockType.TEXT_PARAGRAPH,
                            text = "",
                            orderIndex = blocks.size
                        )
                        blocks = blocks + newBlock
                        showAddMoreSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // IMAGE EDITOR DIALOG (FOR HERO IMAGE)
    val heroImgToEdit = heroImageUrl
    if (editingHeroImage && heroImgToEdit != null) {
        ImageEditorDialog(
            imageUrl = heroImgToEdit,
            initialRotation = heroRotation,
            initialScale = heroScale,
            initialAspect = heroAspect,
            onDismiss = { editingHeroImage = false },
            onSave = { rot, sc, asp ->
                heroRotation = rot
                heroScale = sc
                heroAspect = asp
                editingHeroImage = false
            }
        )
    }

    // IMAGE EDITOR DIALOG (FOR BLOCK PHOTOS)
    val editingIdx = editingImageBlockIndex
    if (editingIdx != null) {
        if (editingIdx in blocks.indices && blocks[editingIdx].imageUrl.isNotBlank()) {
            val b = blocks[editingIdx]
            ImageEditorDialog(
                imageUrl = b.imageUrl,
                initialRotation = b.imageRotation,
                initialScale = b.imageScale,
                initialAspect = b.imageCropAspect,
                onDismiss = { editingImageBlockIndex = null },
                onSave = { rot, sc, asp ->
                    val updated = blocks.toMutableList()
                    updated[editingIdx] = updated[editingIdx].copy(
                        imageRotation = rot,
                        imageScale = sc,
                        imageCropAspect = asp
                    )
                    blocks = updated
                    editingImageBlockIndex = null
                }
            )
        } else {
            editingImageBlockIndex = null
        }
    }

    // BUTTON STYLER SHEET
    val blockToStyle = stylingBlock
    if (blockToStyle != null) {
        ButtonStylerSheet(
            block = blockToStyle,
            onDismiss = { stylingBlock = null },
            onSave = { updated ->
                val list = blocks.toMutableList()
                val idx = list.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    list[idx] = updated
                    blocks = list
                }
                stylingBlock = null
            }
        )
    }

    // INDIVIDUAL BLOCK PREVIEW MODAL
    val blockToPreview = previewingSingleBlock
    if (blockToPreview != null) {
        AlertDialog(
            onDismissRequest = { previewingSingleBlock = null },
            title = { Text("Item Live Preview") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (blockToPreview.type) {
                        BlockType.LINK_BUTTON -> {
                            val btnBg = Color(blockToPreview.buttonBgColor)
                            val btnText = Color(blockToPreview.buttonTextColor)
                            val btnStroke = Color(blockToPreview.buttonStrokeColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(blockToPreview.buttonShadowElevation.dp, RoundedCornerShape(blockToPreview.buttonCornerRadius.dp))
                                    .clip(RoundedCornerShape(blockToPreview.buttonCornerRadius.dp))
                                    .background(btnBg)
                                    .then(
                                        if (blockToPreview.buttonStrokeWidth > 0) Modifier.border(
                                            blockToPreview.buttonStrokeWidth.dp,
                                            btnStroke,
                                            RoundedCornerShape(blockToPreview.buttonCornerRadius.dp)
                                        ) else Modifier
                                    )
                                    .padding(vertical = 14.dp, horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (blockToPreview.text.isNotBlank()) blockToPreview.text else "Click Here",
                                    color = btnText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        BlockType.PHOTO -> {
                            if (blockToPreview.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = blockToPreview.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .scale(blockToPreview.imageScale)
                                        .rotate(blockToPreview.imageRotation)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                        BlockType.COPY_BOX -> {
                            PromptCopyBoxView(
                                promptText = if (blockToPreview.copyPromptText.isNotBlank()) blockToPreview.copyPromptText else blockToPreview.text,
                                buttonLabel = blockToPreview.copyButtonLabel,
                                isDarkMode = isDarkMode
                            )
                        }
                        else -> {
                            Text(text = if (blockToPreview.text.isNotBlank()) blockToPreview.text else "(Empty block)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewingSingleBlock = null }) {
                    Text("Close")
                }
            }
        )
    }

    // AI DESCRIPTION GENERATOR DIALOG (100 to 1000 words limit)
    if (showAiGeneratorDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGeneratingAi) showAiGeneratorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF673AB7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Post Description with AI")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Specify your post topic or keywords, and set the custom word limit from 100 to 1000 words.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = if (aiTopic.isNotBlank()) aiTopic else title,
                        onValueChange = { aiTopic = it },
                        label = { Text("Topic or Brief Prompt") },
                        placeholder = { Text("e.g. Best AI Image Prompt Techniques 2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Target Word Limit: $aiWordLimit words",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = aiWordLimit.toFloat(),
                        onValueChange = { aiWordLimit = it.toInt() },
                        valueRange = 100f..1000f,
                        steps = 17,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100 words", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("500 words", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("1000 words", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }

                    if (isGeneratingAi) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating with Gemini AI...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGeneratingAi = true
                        coroutineScope.launch {
                            val prompt = if (aiTopic.isNotBlank()) aiTopic else title
                            val result = geminiService.generateDescription(prompt, aiWordLimit)
                            when (result) {
                                is AiGenerateResult.Success -> {
                                    description = result.text
                                    showAiGeneratorDialog = false
                                    Toast.makeText(context, "AI description added!", Toast.LENGTH_SHORT).show()
                                }
                                is AiGenerateResult.Error -> {
                                    Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            isGeneratingAi = false
                        }
                    },
                    enabled = !isGeneratingAi,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Text("Generate & Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAiGeneratorDialog = false },
                    enabled = !isGeneratingAi
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // FULL LIVE WYSIWYG PREVIEW DIALOG
    if (showLivePreview) {
        LivePreviewDialog(
            title = title,
            description = description,
            heroImageUrl = heroImageUrl,
            heroImageRotation = heroRotation,
            heroImageScale = heroScale,
            blocks = blocks,
            isDarkModeDefault = isDarkMode,
            onDismiss = { showLivePreview = false },
            onPublishClicked = { doPublish() }
        )
    }

    // PUBLISH RESULT CONFIRMATION DIALOG
    val pResult = publishResultDialog
    if (pResult != null) {
        AlertDialog(
            onDismissRequest = { publishResultDialog = null },
            icon = {
                Icon(
                    if (pResult is PublishResult.Success) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (pResult is PublishResult.Success) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(if (pResult is PublishResult.Success) "Post Published to Blogger!" else "Blogger Status Notice")
            },
            text = {
                when (pResult) {
                    is PublishResult.Success -> {
                        Column {
                            Text(
                                text = "Your post has been successfully published to your Blogger web!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Blogger Post ID: ${pResult.postId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            if (pResult.url.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "URL: ${pResult.url}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                    is PublishResult.Error -> {
                        Text(
                            text = pResult.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        publishResultDialog = null
                        onNavigateToHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("View in Post History")
                }
            },
            dismissButton = {
                if (pResult is PublishResult.Error && pResult.message.contains("Settings", ignoreCase = true)) {
                    TextButton(onClick = {
                        publishResultDialog = null
                        onNavigateToSettings()
                    }) {
                        Text("Open Settings")
                    }
                } else {
                    TextButton(onClick = { publishResultDialog = null }) {
                        Text("Continue Editing")
                    }
                }
            }
        )
    }
}
