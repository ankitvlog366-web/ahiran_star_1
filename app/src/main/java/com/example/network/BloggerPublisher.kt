package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class PublishResult {
    data class Success(val postId: String, val url: String, val title: String) : PublishResult()
    data class Error(val message: String) : PublishResult()
}

sealed class ConnectionResult {
    data class Success(val blogId: String, val blogName: String, val postsCount: Int, val blogUrl: String) : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

data class BlogInfo(
    val id: String,
    val name: String,
    val url: String
)

class BloggerPublisher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun publishPost(
        accessToken: String,
        blogId: String,
        title: String,
        contentHtml: String,
        isDraft: Boolean = false,
        labels: List<String> = emptyList()
    ): PublishResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext PublishResult.Error("Blogger API Token is empty. Please configure your token in Settings.")
        }
        if (blogId.isBlank()) {
            return@withContext PublishResult.Error("Blogger Blog ID is empty. Please configure your Blog ID in Settings.")
        }

        try {
            val url = "https://www.googleapis.com/blogger/v3/blogs/$blogId/posts/?isDraft=$isDraft"
            val payload = JSONObject().apply {
                put("kind", "blogger#post")
                put("title", title.ifBlank { "Untitled Post" })
                put("content", contentHtml)
                if (labels.isNotEmpty()) {
                    val lblArray = JSONArray()
                    labels.forEach { lblArray.put(it) }
                    put("labels", lblArray)
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val postId = json.optString("id", "")
                val postUrl = json.optString("url", "")
                val postTitle = json.optString("title", title)
                PublishResult.Success(postId, postUrl, postTitle)
            } else {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    val errObj = errJson.optJSONObject("error")
                    errObj?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                PublishResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            PublishResult.Error("Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun updatePost(
        accessToken: String,
        blogId: String,
        postId: String,
        title: String,
        contentHtml: String
    ): PublishResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank() || blogId.isBlank() || postId.isBlank()) {
            return@withContext PublishResult.Error("Missing access token, blog ID, or post ID for update.")
        }

        try {
            val url = "https://www.googleapis.com/blogger/v3/blogs/$blogId/posts/$postId"
            val payload = JSONObject().apply {
                put("id", postId)
                put("title", title)
                put("content", contentHtml)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val postUrl = json.optString("url", "")
                PublishResult.Success(postId, postUrl, title)
            } else {
                PublishResult.Error("HTTP ${response.code}: $responseBody")
            }
        } catch (e: Exception) {
            PublishResult.Error("Update failed: ${e.localizedMessage}")
        }
    }

    suspend fun testConnection(accessToken: String, blogId: String): ConnectionResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext ConnectionResult.Error("Blogger Access Token is required.")
        }
        if (blogId.isBlank()) {
            return@withContext ConnectionResult.Error("Blog ID is required.")
        }

        try {
            val url = "https://www.googleapis.com/blogger/v3/blogs/$blogId"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val name = json.optString("name", "Blogger Blog")
                val blogUrl = json.optString("url", "")
                val postsObj = json.optJSONObject("posts")
                val totalItems = postsObj?.optInt("totalItems", 0) ?: 0
                ConnectionResult.Success(blogId, name, totalItems, blogUrl)
            } else {
                val errMessage = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                ConnectionResult.Error(errMessage)
            }
        } catch (e: Exception) {
            ConnectionResult.Error("Connection test failed: ${e.localizedMessage}")
        }
    }

    suspend fun fetchUserBlogs(accessToken: String): List<BlogInfo> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext emptyList()
        try {
            val url = "https://www.googleapis.com/blogger/v3/users/self/blogs"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                val list = mutableListOf<BlogInfo>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    list.add(
                        BlogInfo(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            url = item.optString("url")
                        )
                    )
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
