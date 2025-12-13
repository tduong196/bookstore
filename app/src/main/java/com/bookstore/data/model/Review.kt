package com.bookstore.data.model

data class Review(
    var id: String = "",
    var orderId: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var userName: String = "",
    var bookId: String = "",
    var bookTitle: String = "",
    var rating: Double = 0.0,
    var comment: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var approved: Boolean = false
)

