package com.scalemart.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scalemart.api.domain.Product;
import com.scalemart.api.domain.Review;
import com.scalemart.api.dto.CreateReviewRequest;
import com.scalemart.api.dto.ReviewResponse;
import com.scalemart.api.repository.ProductRepository;
import com.scalemart.api.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ReviewResponse createReview(String userId, Long productId, CreateReviewRequest request) {
        // Check if product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        // Check if already reviewed
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }
        
        Review review = new Review();
        review.setUserId(userId);
        review.setProduct(product);
        review.setRating(request.rating());
        review.setComment(request.comment());
        
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return reviews.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewResponse getUserReview(String userId, Long productId) {
        return reviewRepository.findByUserIdAndProductId(userId, productId)
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public ReviewResponse updateReview(String userId, Long productId, CreateReviewRequest request) {
        Review review = reviewRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        
        review.setRating(request.rating());
        review.setComment(request.comment());
        
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional
    public void deleteReview(String userId, Long productId) {
        Review review = reviewRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }

    @Transactional(readOnly = true)
    public Long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUserId(),
            review.getProduct().getId(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
