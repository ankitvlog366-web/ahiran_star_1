package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blogger_studio_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_ADMIN_USER = "officialankityadav1"
        const val DEFAULT_ADMIN_PASS = "Ankit13@"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var loggedInUser: String
        get() = prefs.getString("logged_in_user", "") ?: ""
        set(value) = prefs.edit().putString("logged_in_user", value).apply()

    var googleClientId: String
        get() = prefs.getString("google_client_id", "") ?: ""
        set(value) = prefs.edit().putString("google_client_id", value).apply()

    var googleClientSecret: String
        get() = prefs.getString("google_client_secret", "") ?: ""
        set(value) = prefs.edit().putString("google_client_secret", value).apply()

    var bloggerAccessToken: String
        get() = prefs.getString("blogger_access_token", "") ?: ""
        set(value) = prefs.edit().putString("blogger_access_token", value).apply()

    var bloggerRefreshToken: String
        get() = prefs.getString("blogger_refresh_token", "") ?: ""
        set(value) = prefs.edit().putString("blogger_refresh_token", value).apply()

    var bloggerBlogId: String
        get() = prefs.getString("blogger_blog_id", "") ?: ""
        set(value) = prefs.edit().putString("blogger_blog_id", value).apply()

    var bloggerBlogName: String
        get() = prefs.getString("blogger_blog_name", "My Blogger Blog") ?: "My Blogger Blog"
        set(value) = prefs.edit().putString("blogger_blog_name", value).apply()

    var bloggerBlogUrl: String
        get() = prefs.getString("blogger_blog_url", "") ?: ""
        set(value) = prefs.edit().putString("blogger_blog_url", value).apply()

    var callbackUrl: String
        get() = prefs.getString("callback_url", "https://localhost") ?: "https://localhost"
        set(value) = prefs.edit().putString("callback_url", value).apply()

    var externalApiUrl: String
        get() = prefs.getString("external_api_url", "") ?: ""
        set(value) = prefs.edit().putString("external_api_url", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    fun isBloggerConnected(): Boolean {
        return bloggerAccessToken.isNotBlank() && bloggerBlogId.isNotBlank()
    }
}
