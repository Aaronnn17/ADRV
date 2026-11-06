package com.example.adrv.model

data class ApiNewsResponse(
    val updatedAt: String,
    val totalNews: Int,
    val news: List<News>
)

data class News(
    val id: String,
    val title: String,
    val link: String,
    val published: String,
    val summary: String,
    val content: String,
    val categories: List<String>,
    val image: String?
)