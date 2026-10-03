package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bloggerPostId: String? = null,
    val bloggerUrl: String? = null,
    val title: String,
    val heroImageUrl: String? = null,
    val heroImageRotation: Float = 0f,
    val heroImageScale: Float = 1f,
    val heroImageCropAspect: String = "16:9",
    val description: String = "",
    val blocksJson: String = "[]",
    val status: String = "DRAFT", // "PUBLISHED", "DRAFT", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val labels: String = ""
)
