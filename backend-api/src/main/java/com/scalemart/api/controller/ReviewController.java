package com.scalemart.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.scalemart.api.dto.CreateReviewRequest;
import com.scalemart.api.dto.ReviewResponse;
import com.scalemart.api.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}")
    public List<ReviewResponse> getProductReviews(@PathVariable Long productId) {
        return reviewService.getProductReviews(productId);
    }

    @GetMapping("/product/{productId}/average")
    public Double getAverageRating(@PathVariable Long productId) {
        return reviewService.getAverageRating(productId);
    }

    @GetMapping("/product/{productId}/count")
    public Long getReviewCount(@PathVariable Long productId) {
        return reviewService.getReviewCount(productId);
    }

    @PostMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
        @PathVariable Long productId,
        @Valid @RequestBody CreateReviewRequest request,
        java.security.Principal principal) {
        return reviewService.createReview(principal.getName(), productId, request);
    }

    @PutMapping("/product/{productId}")
    public ReviewResponse updateReview(
        @PathVariable Long productId,
        @Valid @RequestBody CreateReviewRequest request,
        java.security.Principal principal) {
        return reviewService.updateReview(principal.getName(), productId, request);
    }

    @DeleteMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
        @PathVariable Long productId,
        java.security.Principal principal) {
        reviewService.deleteReview(principal.getName(), productId);
    }
}
