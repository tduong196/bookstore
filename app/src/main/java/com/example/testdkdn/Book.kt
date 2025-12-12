package com.example.testdkdn
data class Book(
    var id: String = "",
    var title: String = "",
    var author: String = "",
    var description: String = "",
    var category: String = "",
    var rating: Double = 0.0,
    var image_url: String = "",
    var price: Double = 0.0,
    var quantity: Int = 1
)
