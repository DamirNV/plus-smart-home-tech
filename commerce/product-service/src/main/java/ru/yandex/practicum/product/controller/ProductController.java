package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDto> getByCategory(@PathVariable Long categoryId) {
        return productService.getByCategory(categoryId);
    }

    @GetMapping("/search")
    public List<ProductDto> search(@RequestParam String query) {
        return productService.search(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}")
    public ProductDto update(@PathVariable Long id,
                             @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }
}
