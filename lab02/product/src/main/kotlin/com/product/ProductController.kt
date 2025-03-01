package com.product

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// https://kotlinlang.org/docs/jvm-spring-boot-add-data-class.html#update-your-application
// там используют бд, но для этого дз можно просто все хранить в списке

@RestController
@RequestMapping("/")
class ProductController {
    private val products = mutableListOf<Product>()
    private var nextId = 0
    private val imagesFolder = Files.createDirectories(Path.of("images"))


    @GetMapping("/product/{id}")
    fun getProduct(@PathVariable id: Int): Product {
        return products.find { it.id == id } ?: throw Exception("GET: Product doesn't exist")
    }

    @GetMapping("/product/{id}/image", produces = ["image/png"])
    fun getImage(@PathVariable id: Int): ByteArray {
        val product = products.find { it.id == id } ?: throw Exception("GET IMAGE: Product doesn't exist")
        product.imagePath ?: throw Exception("GET IMAGE: Image path doesn't exist")
        val imagePath = Path.of(product.imagePath!!) ?: throw Exception("GET IMAGE: Image doesn't exist")
        return Files.readAllBytes(imagePath)
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
        val product = Product(++nextId, constructor.name, constructor.description, "")
        products.add(product)
        return product
    }

    @PostMapping("/product/{id}/image")
    fun addImage(@PathVariable id: Int, @RequestParam("image") file: MultipartFile): String {
        val product = products.find { it.id == id } ?: throw Exception("ADD IMAGE: Product doesn't exist")
        val imageFilePath = imagesFolder.resolve("product_${id}")
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, imageFilePath, StandardCopyOption.REPLACE_EXISTING)
        }
        product.imagePath = imageFilePath.toString()
        return imageFilePath.fileName.toString()
    }

    @PutMapping("/product/{id}")
    fun updateProduct(@PathVariable id: Int, @RequestBody productModifier: ProductModifier): Product {
        val product = products.find { it.id == id } ?: throw Exception("PUT: Product doesn't exist")
        product.name = productModifier.name ?: product.name
        product.description = productModifier.description ?: product.description
        return product
    }
}
