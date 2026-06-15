package com.mserapinas.boardgame.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecommenderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RecommenderEventPublisher.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RecommenderEventPublisher(
        RestTemplate restTemplate,
        @Value("${recommender.service.url:http://localhost:3004}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Async
    public void publishCollectionChanged(Long userId) {
        post("/api/v1/events/user_collection_changed", userId);
    }

    @Async
    public void publishReviewChanged(Long userId) {
        post("/api/v1/events/user_review_changed", userId);
    }

    private void post(String path, Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Long>> entity = new HttpEntity<>(Map.of("user_id", userId), headers);
            restTemplate.postForEntity(baseUrl + path, entity, Void.class);
        } catch (Exception e) {
            log.warn("Failed to publish to recommender-service {} for user {}: {}", path, userId, e.getMessage());
        }
    }
}
