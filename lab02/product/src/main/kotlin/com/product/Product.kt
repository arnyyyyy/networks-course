package com.product

data class Product(
    val id: Int,
    var name: String,
    var description: String,
    var imagePath : String?
)


data class ProductConstructor(
    val name: String,
    val description: String,
)

data class ProductModifier(
    val name: String?,
    val description: String?
)