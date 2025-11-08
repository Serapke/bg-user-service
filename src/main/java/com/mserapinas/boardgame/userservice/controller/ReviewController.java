package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.CreateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.response.ReviewDto;
import com.mserapinas.boardgame.userservice.dto.response.ReviewListDto;
import com.mserapinas.boardgame.userservice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @CurrentUser Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewDto review = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> getReviewById(@PathVariable Long reviewId) {
        ReviewDto review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReviewDto>> getCurrentUserReviews(@CurrentUser Long userId) {
        List<ReviewDto> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<ReviewListDto> getReviewsByGame(@PathVariable Integer gameId) {
        ReviewListDto reviews = reviewService.getReviewsByGame(gameId);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @CurrentUser Long userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewDto updatedReview = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @CurrentUser Long userId,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(userId, reviewId);
    }
}