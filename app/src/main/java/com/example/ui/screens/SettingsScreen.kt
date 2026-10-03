package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlogPostEntity
import com.example.data.model.PostBlockJsonHelper
import com.example.data.preferences.AppPreferences
import com.example.network.ArticleExtractor
import com.example.network.BloggerPublisher
import com.example.network.ConnectionResult
import com.example.network.PublishResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onLoadExtractedArticle: (BlogPostEntity) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val publisher = remember { BloggerPublisher() }
    val extractor = remember { ArticleExtractor() }

    // Configuration states
    var googleClientId by remember { mutableStateOf(appPreferences.googleClientId) }
    var googleClientSecret by remember { mutableStateOf(appPreferences.googleClientSecret) }
    var bloggerToken by remember { mutableStateOf(appPreferences.bloggerAccessToken) }
    var bloggerRefreshToken by remember { mutableStateOf(appPreferences.bloggerRefreshToken) }
    var blogId by remember { mutableStateOf(appPreferences.bloggerBlogId) }
    var blogName by remember { mutableStateOf(appPreferences.bloggerBlogName) }
    var callbackUrl by remember { mutableStateOf(appPreferences.callbackUrl) }

    // External article extraction
    var externalUrl by remember { mutableStateOf(appPreferences.externalApiUrl) }
    var isExtracting by remember { mutableStateOf(false) }

    // Connection testing
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }

    // Test post testing
    var isSendingTestPost by remember { mutableStateOf(false) }
    var testPostResult by remember { mutableStateOf<String?>(null) }

    val saveSettings = {
        appPreferences.googleClientId = googleClientId.trim()
        appPreferences.googleClientSecret = googleClientSecret.trim()
        appPreferences.bloggerAccessToken = bloggerToken.trim()
        appPreferences.bloggerRefreshToken = bloggerRefreshToken.trim()
        appPreferences.bloggerBlogId = blogId.trim()
        appPreferences.bloggerBlogName = blogName.trim()
        appPreferences.callbackUrl = callbackUrl.trim()
        appPreferences.externalApiUrl = externalUrl.trim()
        Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blogger & API Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    Button(
                        onClick = { saveSettings() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // BLOGGER STATUS BANNER
            val isConnected = appPreferences.isBloggerConnected()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConnected) Icons.Default.CheckCircle else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "Blogger Connected & Active" else "Blogger Not Connected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                        Text(
                            text = if (isConnected) "Blog: ${appPreferences.bloggerBlogName} (ID: ${appPreferences.bloggerBlogId})"
                            else "Add your Blogger API Token & Blog ID below to publish directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFD84315)
                        )
                    }
                }
            }

            // 1. GOOGLE CLIENT ID & SECRET (OAUTH CREDENTIALS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFF5722))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google OAuth 2.0 Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configure your Google Cloud Client ID & Secret for Blogger API v3 access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = googleClientId,
                        onValueChange = { googleClientId = it },
                        label = { Text("Google Client ID") },
                        placeholder = { Text("xxxxxx.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = googleClientSecret,
                        onValueChange = { googleClientSecret = it },
                        label = { Text("Google Client Secret") },
                        placeholder = { Text("GOCSPX-xxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = callbackUrl,
                        onValueChange = { callbackUrl = it },
                        label = { Text("OAuth Callback / Redirect URL") },
                        placeholder = { Text("https://localhost") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. BLOGGER TOKEN & BLOG IDENTIFIER
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFFF5722))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blogger Token & Blog ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter your Blogger OAuth Access Token / Bearer Token and Blog ID. You can change this anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = bloggerToken,
                        onValueChange = { bloggerToken = it },
                        label = { Text("Blogger Access Token / API Token") },
                        placeholder = { Text("ya29.a0AfH6SM...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bloggerRefreshToken,
                        onValueChange = { bloggerRefreshToken = it },
                        label = { Text("Blogger Refresh Token (Persistent Connection)") },
                        placeholder = { Text("1//04xxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = blogId,
                            onValueChange = { blogId = it },
                            label = { Text("Blogger Blog ID") },
                            placeholder = { Text("e.g. 847047023250") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = blogName,
                            onValueChange = { blogName = it },
                            label = { Text("Blog Name") },
                            placeholder = { Text("My Blog") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Connection Button & Send Test Post
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                saveSettings()
                                isTestingConnection = true
                                connectionStatusMessage = null
                                coroutineScope.launch {
                                    val res = publisher.testConnection(bloggerToken.trim(), blogId.trim())
                                    isTestingConnection = false
                                    when (res) {
                                        is ConnectionResult.Success -> {
                                            isConnectionSuccess = true
                                            blogName = res.blogName
                                            appPreferences.bloggerBlogName = res.blogName
                                            connectionStatusMessage = "Success! Connected to '${res.blogName}'. Total Posts: ${res.postsCount}"
                                        }
                                        is ConnectionResult.Error -> {
                                            isConnectionSuccess = false
                                            connectionStatusMessage = "Connection failed: ${res.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }
                        }

                        Button(
                            onClick = {
                                saveSettings()
                                isSendingTestPost = true
                                testPostResult = null
                                coroutineScope.launch {
                                    val testHtml = "<p>This is a test post from Blogger Studio on Android to verify API connectivity.</p>"
                                    val res = publisher.publishPost(
                                        accessToken = bloggerToken.trim(),
                                        blogId = blogId.trim(),
                                        title = "Blogger Studio Test Post",
                                        contentHtml = testHtml,
                                        isDraft = true
                                    )
                                    isSendingTestPost = false
                                    when (res) {
                                        is PublishResult.Success -> {
                                            testPostResult = "Test post published to Blogger! (Post ID: ${res.postId})"
                                        }
                                        is PublishResult.Error -> {
                                            testPostResult = "Test failed: ${res.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isSendingTestPost,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSendingTestPost) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Posting...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Post", color = Color.White)
                            }
                        }
                    }

                    // Status Messages
                    if (connectionStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = connectionStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnectionSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (testPostResult != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = testPostResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testPostResult?.startsWith("Test post") == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. EXTERNAL WEBSITE POST EXTRACTOR ("dusre website se kiya huaa post ka bhi bhi post ko extrack katke edit kar paye")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF009688))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "External Website Post Extractor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extract article content, title, and images from any external website link or API to edit and publish on your Blogger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = externalUrl,
                        onValueChange = { externalUrl = it },
                        label = { Text("Article Link / API URL") },
                        placeholder = { Text("https://example.com/article-slug") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (externalUrl.isBlank()) {
                                Toast.makeText(context, "Please enter a valid website URL or API link.", Toast.LENGTH_SHORT).show()
                            } else {
                                isExtracting = true
                                coroutineScope.launch {
                                    val extracted = extractor.extractArticle(externalUrl.trim())
                                    isExtracting = false
                                    val entity = BlogPostEntity(
                                        title = extracted.title,
                                        description = extracted.description,
                                        heroImageUrl = extracted.heroImageUrl,
                                        blocksJson = PostBlockJsonHelper.toJson(extracted.blocks),
                                        status = "DRAFT"
                                    )
                                    Toast.makeText(context, "Article extracted! Loading in Editor...", Toast.LENGTH_SHORT).show()
                                    onLoadExtractedArticle(entity)
                                }
                            }
                        },
                        enabled = !isExtracting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting Article & Images...", color = Color.White)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract & Load into Editor", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. ADMIN USER & LOGOUT
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Admin Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Signed in as: ${appPreferences.loggedInUser.ifBlank { AppPreferences.DEFAULT_ADMIN_USER }}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            appPreferences.isLoggedIn = false
                            onLogout()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out of Admin Panel")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
