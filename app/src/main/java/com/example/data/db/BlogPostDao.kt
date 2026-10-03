package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlogPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlogPostDao {
    @Query("SELECT * FROM blog_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE id = :id")
    suspend fun getPostById(id: Long): BlogPostEntity?

    @Query("SELECT * FROM blog_posts WHERE bloggerPostId = :bloggerPostId LIMIT 1")
    suspend fun getPostByBloggerId(bloggerPostId: String): BlogPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BlogPostEntity): Long

    @Update
    suspend fun updatePost(post: BlogPostEntity)

    @Delete
    suspend fun deletePost(post: BlogPostEntity)

    @Query("DELETE FROM blog_posts WHERE id = :id")
    suspend fun deletePostById(id: Long)
}
