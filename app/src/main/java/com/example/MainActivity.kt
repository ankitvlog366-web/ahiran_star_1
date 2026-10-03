package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BlogRepository
import com.example.ui.screens.DashboardContainerScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appPreferences = AppPreferences(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        val repository = BlogRepository(database.blogPostDao())

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember {
                mutableStateOf(if (appPreferences.isDarkMode) true else systemDark)
            }
            var isLoggedIn by remember {
                mutableStateOf(appPreferences.isLoggedIn)
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isLoggedIn) {
                        LoginScreen(
                            appPreferences = appPreferences,
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    } else {
                        DashboardContainerScreen(
                            appPreferences = appPreferences,
                            blogRepository = repository,
                            onLogout = {
                                isLoggedIn = false
                            }
                        )
                    }
                }
            }
        }
    }
}
