package com.example.data.repository

import com.example.data.db.BlogPostDao
import com.example.data.model.BlogPostEntity
import kotlinx.coroutines.flow.Flow

class BlogRepository(private val dao: BlogPostDao) {

    val allPosts: Flow<List<BlogPostEntity>> = dao.getAllPosts()

    suspend fun getPostById(id: Long): BlogPostEntity? = dao.getPostById(id)

    suspend fun savePost(post: BlogPostEntity): Long {
        return if (post.id == 0L) {
            dao.insertPost(post)
        } else {
            dao.updatePost(post)
            post.id
        }
    }

    suspend fun deletePost(id: Long) {
        dao.deletePostById(id)
    }
}
