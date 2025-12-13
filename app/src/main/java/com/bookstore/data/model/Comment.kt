package com.bookstore.data.model

data class Comment(
    val bookTitle: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val userEmail: String = ""
)
