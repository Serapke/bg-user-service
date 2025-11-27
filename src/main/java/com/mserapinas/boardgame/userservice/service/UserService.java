package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.AddGameToCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGameCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionDto;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionItemDto;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.Label;
import com.mserapinas.boardgame.userservice.model.Review;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.model.UserBoardGame;
import com.mserapinas.boardgame.userservice.repository.LabelRepository;
import com.mserapinas.boardgame.userservice.repository.ReviewRepository;
import com.mserapinas.boardgame.userservice.repository.UserBoardGameRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserBoardGameRepository userBoardGameRepository;
    private final LabelRepository labelRepository;
    private final ReviewRepository reviewRepository;

    public UserService(UserRepository userRepository, UserBoardGameRepository userBoardGameRepository,
                      LabelRepository labelRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.userBoardGameRepository = userBoardGameRepository;
        this.labelRepository = labelRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public User updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(InvalidCredentialsException::new);

        user.setName(request.name());
        return userRepository.save(user);
    }

    public GameCollectionDto getUserGameCollection(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidCredentialsException();
        }

        List<UserBoardGame> userBoardGames = userBoardGameRepository.findByUserIdWithLabels(userId);

        List<Review> userReviews = reviewRepository.findByUserIdWithUser(userId);

        Map<Integer, Integer> gameRatings = userReviews.stream().collect(toMap(Review::getGameId, Review::getRating));

        List<GameCollectionItemDto> games = userBoardGames.stream()
            .map(userBoardGame -> {
                Integer userRating = gameRatings.get(userBoardGame.getGameId());
                return GameCollectionItemDto.from(userBoardGame, userRating);
            })
            .toList();

        return GameCollectionDto.from(games);
    }

    @Transactional
    public GameCollectionItemDto addGameToCollection(Long userId, AddGameToCollectionRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidCredentialsException();
        }

        if (userBoardGameRepository.existsByUserIdAndGameId(userId, request.gameId())) {
            throw new IllegalArgumentException("Game already exists in your collection");
        }

        UserBoardGame userBoardGame = new UserBoardGame(
            userId,
            request.gameId(),
            request.notes()
        );
        userBoardGame.setModifiedAt(OffsetDateTime.now());

        if (request.labelNames() != null && !request.labelNames().isEmpty()) {
            Set<Label> labels = processLabels(userId, request.labelNames());
            userBoardGame.setLabels(labels);
        }

        UserBoardGame savedGame = userBoardGameRepository.save(userBoardGame);
        Integer userRating = reviewRepository.findByUserIdAndGameId(userId, request.gameId())
            .map(Review::getRating)
            .orElse(null);
        return GameCollectionItemDto.from(savedGame, userRating);
    }

    private Set<Label> processLabels(Long userId, Set<String> labelNames) {
        // Find existing labels
        List<Label> existingLabels = labelRepository.findByUserIdAndNameIn(userId, labelNames);
        Set<String> existingLabelNames = existingLabels.stream()
            .map(Label::getName)
            .collect(java.util.stream.Collectors.toSet());
        
        Set<Label> allLabels = new HashSet<>(existingLabels);
        
        // Create new labels for names that don't exist
        for (String labelName : labelNames) {
            if (!existingLabelNames.contains(labelName)) {
                Label newLabel = new Label(userId, labelName);
                newLabel.setCreatedAt(OffsetDateTime.now());
                Label savedLabel = labelRepository.save(newLabel);
                allLabels.add(savedLabel);
            }
        }
        
        return allLabels;
    }

    @Transactional
    public void deleteGameFromCollection(Long userId, Integer gameId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidCredentialsException();
        }

        if (!userBoardGameRepository.existsByUserIdAndGameId(userId, gameId)) {
            throw new IllegalArgumentException("Game not found in your collection");
        }

        userBoardGameRepository.deleteByUserIdAndGameId(userId, gameId);
    }

    @Transactional
    public GameCollectionItemDto updateGameInCollection(Long userId, Integer gameId, UpdateGameCollectionRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidCredentialsException();
        }

        UserBoardGame userBoardGame = userBoardGameRepository.findByUserIdAndGameIdWithLabels(userId, gameId)
            .orElseThrow(() -> new IllegalArgumentException("Game not found in your collection"));

        userBoardGame.setNotes(request.notes());
        userBoardGame.setModifiedAt(OffsetDateTime.now());

        if (request.labelNames() != null) {
            Set<Label> labels = processLabels(userId, request.labelNames());
            userBoardGame.setLabels(labels);
        }

        UserBoardGame savedGame = userBoardGameRepository.save(userBoardGame);
        Integer userRating = reviewRepository.findByUserIdAndGameId(userId, gameId)
            .map(Review::getRating)
            .orElse(null);
        return GameCollectionItemDto.from(savedGame, userRating);
    }

    @Transactional
    public void deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(InvalidCredentialsException::new);

        // Bulk delete all reviews to avoid N+1 problem and transient object issues
        reviewRepository.deleteByUserId(userId);

        userRepository.delete(user);
    }
}
