package ru.yandex.practicum.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository,
                          CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    public List<ProductDto> getAll() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductDto getById(Long id) {
        return toDto(findEntity(id));
    }

    public List<ProductDto> getByCategory(Long categoryId) {
        categoryService.findEntity(categoryId);

        return productRepository.findAllByCategoryIdAndActiveTrue(categoryId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> search(String query) {
        return productRepository.findAllByNameContainingIgnoreCaseAndActiveTrue(query)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Product product = new Product();

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        if (request.categoryId() != null) {
            product.setCategory(categoryService.findEntity(request.categoryId()));
        }
        product.setImageUrl(request.imageUrl());
        product.setActive(true);

        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = findEntity(id);

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.categoryId() != null) {
            if (request.categoryId() != null) {
            product.setCategory(categoryService.findEntity(request.categoryId()));
        }
        }

        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }

        if (request.active() != null) {
            product.setActive(request.active());
        }

        return toDto(productRepository.save(product));
    }

    private Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory() == null
                        ? null
                        : categoryService.toDto(product.getCategory()),
                product.getImageUrl(),
                product.isActive()
        );
    }
}
