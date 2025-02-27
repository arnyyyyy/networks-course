package com.product

import org.springframework.web.bind.annotation.*

// https://kotlinlang.org/docs/jvm-spring-boot-add-data-class.html#update-your-application
// там используют бд, но для этого дз можно просто все хранить в списке

@RestController
@RequestMapping("/")
class ProductController {
    private val products = mutableListOf<Product>()
    private var nextId = 0

    @GetMapping("/product/{id}")
    fun getProduct(@PathVariable id: Int): Product {
        return products.find { it.id == id } ?: throw Exception("GET: Product doesn't exist")
    }

    @GetMapping("/products")
    fun getAllProducts(): List<Product> = products


    @DeleteMapping("/product/{id}")
    fun deleteProduct(@PathVariable id: Int): Product {
        val product = products.find { it.id == id } ?: throw Exception("DELETE: Product doesn't exist")
        products.remove(product)
        return product
    }

    @PostMapping("/product")
    fun addProduct(@RequestBody constructor: ProductConstructor): Product {
        val product = Product(++nextId, constructor.name, constructor.description)
        products.add(product)
        return product
    }

    @PutMapping("/product/{id}")
    fun updateProduct(@PathVariable id: Int, @RequestBody productModifier: ProductModifier): Product {
        val product = products.find { it.id == id } ?: throw Exception("PUT: Product doesn't exist")
        product.name = productModifier.name ?: product.name
        product.description = productModifier.description ?: product.description
        return product
    }
}
