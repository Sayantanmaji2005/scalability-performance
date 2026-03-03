package com.scalemart.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.scalemart.api.domain.Product;
import com.scalemart.api.dto.ProductResponse;
import com.scalemart.api.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "productById", key = "#id")
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(product);
    }

    @Cacheable(cacheNames = "trendingProducts")
    public List<ProductResponse> getTrending() {
        return productRepository.findTop10ByOrderByUpdatedAtDesc()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getCategory(),
            product.getPrice(),
            product.getStock(),
            product.getImageUrl(),
            product.getAverageRating(),
            product.getReviewCount(),
            product.getIsActive());
    }

    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategory(category)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ProductResponse> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // Pagination support
    public Page<ProductResponse> getProductsPaged(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(this::toResponse);
    }

    public Page<ProductResponse> searchProductsPaged(
            String name,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.searchProducts(name, category, minPrice, maxPrice, pageable)
            .map(this::toResponse);
    }
}
