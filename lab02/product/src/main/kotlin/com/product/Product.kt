package com.product

data class Product(
    var id: Int,
    var name: String,
    var description: String
)


data class ProductConstructor(
    val name: String,
    val description: String
)

data class ProductModifier(
    val name: String?,
    val description: String?
)