package com.scalemart.api.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scalemart.api.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findTop10ByOrderByUpdatedAtDesc();
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);
    
    // Pagination support
    @SuppressWarnings("override")
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // Search with filters
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
        @Param("name") String name,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable);
}
