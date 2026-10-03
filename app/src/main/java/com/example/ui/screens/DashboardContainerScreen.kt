package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlogPostEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BlogRepository

enum class DashboardTab {
    EDITOR,
    HISTORY,
    SETTINGS
}

@Composable
fun DashboardContainerScreen(
    appPreferences: AppPreferences,
    blogRepository: BlogRepository,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.EDITOR) }
    var currentEditingPost by remember { mutableStateOf<BlogPostEntity?>(null) }

    BackHandler(enabled = selectedTab != DashboardTab.EDITOR || currentEditingPost != null) {
        if (currentEditingPost != null) {
            currentEditingPost = null
        } else {
            selectedTab = DashboardTab.EDITOR
        }
    }

    Scaffold(
        topBar = {
            // Quick Top "Connect Blog" banner strip if blog is not connected yet
            val isConnected = appPreferences.isBloggerConnected()
            if (!isConnected) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blogger not connected yet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBF360C)
                            )
                        }

                        Button(
                            onClick = { selectedTab = DashboardTab.SETTINGS },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Connect Blog", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("dashboard_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.EDITOR,
                    onClick = {
                        selectedTab = DashboardTab.EDITOR
                    },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "Editor") },
                    label = { Text("Editor") },
                    modifier = Modifier.testTag("tab_editor")
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.HISTORY,
                    onClick = {
                        selectedTab = DashboardTab.HISTORY
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("Post History") },
                    modifier = Modifier.testTag("tab_history")
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SETTINGS,
                    onClick = {
                        selectedTab = DashboardTab.SETTINGS
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DashboardTab.EDITOR -> {
                    key(currentEditingPost?.id ?: 0L) {
                        EditorScreen(
                            appPreferences = appPreferences,
                            blogRepository = blogRepository,
                            editingPost = currentEditingPost,
                            onNavigateToHistory = {
                                selectedTab = DashboardTab.HISTORY
                            },
                            onNavigateToSettings = {
                                selectedTab = DashboardTab.SETTINGS
                            }
                        )
                    }
                }
                DashboardTab.HISTORY -> {
                    HistoryScreen(
                        blogRepository = blogRepository,
                        onEditPost = { post ->
                            currentEditingPost = post
                            selectedTab = DashboardTab.EDITOR
                        },
                        onCreateNewPost = {
                            currentEditingPost = null
                            selectedTab = DashboardTab.EDITOR
                        }
                    )
                }
                DashboardTab.SETTINGS -> {
                    SettingsScreen(
                        appPreferences = appPreferences,
                        onLoadExtractedArticle = { extractedPost ->
                            currentEditingPost = extractedPost
                            selectedTab = DashboardTab.EDITOR
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
