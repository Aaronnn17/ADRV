package com.example.adrv.model

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val date: String,
    val sourceName: String = "Pasión por el Rayo",
    val imageUrl: String? = null,
    val articleUrl: String? = null,
    val isFeatured: Boolean = false
)