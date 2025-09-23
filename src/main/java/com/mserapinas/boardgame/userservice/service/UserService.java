package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.AddGameToCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGameCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionDto;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionItemDto;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.Label;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.model.UserBoardGame;
import com.mserapinas.boardgame.userservice.repository.LabelRepository;
import com.mserapinas.boardgame.userservice.repository.UserBoardGameRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserBoardGameRepository userBoardGameRepository;
    private final LabelRepository labelRepository;
    private final UserContextService userContextService;

    public UserService(UserRepository userRepository, UserBoardGameRepository userBoardGameRepository, LabelRepository labelRepository, UserContextService userContextService) {
        this.userRepository = userRepository;
        this.userBoardGameRepository = userBoardGameRepository;
        this.labelRepository = labelRepository;
        this.userContextService = userContextService;
    }

    @Transactional
    public User updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        currentUser.setName(request.name());
        return userRepository.save(currentUser);
    }

    public GameCollectionDto getCurrentUserGameCollection() {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        List<GameCollectionItemDto> games = userBoardGameRepository.findByUserIdWithLabels(currentUser.getId())
            .stream()
            .map(GameCollectionItemDto::from)
            .toList();
        
        return GameCollectionDto.from(games);
    }

    @Transactional
    public GameCollectionItemDto addGameToCollection(AddGameToCollectionRequest request) {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        // Check if game already exists in user's collection (efficient query)
        if (userBoardGameRepository.existsByUserIdAndGameId(currentUser.getId(), request.gameId())) {
            throw new IllegalArgumentException("Game already exists in your collection");
        }
        
        UserBoardGame userBoardGame = new UserBoardGame(
            currentUser.getId(), 
            request.gameId(), 
            request.notes()
        );
        userBoardGame.setModifiedAt(OffsetDateTime.now());
        
        // Handle labels if provided
        if (request.labelNames() != null && !request.labelNames().isEmpty()) {
            Set<Label> labels = processLabels(currentUser.getId(), request.labelNames());
            userBoardGame.setLabels(labels);
        }
        
        UserBoardGame savedGame = userBoardGameRepository.save(userBoardGame);
        return GameCollectionItemDto.from(savedGame);
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
    public void deleteGameFromCollection(Integer gameId) {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        // Check if game exists in user's collection
        if (!userBoardGameRepository.existsByUserIdAndGameId(currentUser.getId(), gameId)) {
            throw new IllegalArgumentException("Game not found in your collection");
        }
        
        userBoardGameRepository.deleteByUserIdAndGameId(currentUser.getId(), gameId);
    }

    @Transactional
    public GameCollectionItemDto updateGameInCollection(Integer gameId, UpdateGameCollectionRequest request) {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        UserBoardGame userBoardGame = userBoardGameRepository.findByUserIdAndGameIdWithLabels(currentUser.getId(), gameId)
            .orElseThrow(() -> new IllegalArgumentException("Game not found in your collection"));
        
        userBoardGame.setNotes(request.notes());
        userBoardGame.setModifiedAt(OffsetDateTime.now());
        
        if (request.labelNames() != null) {
            Set<Label> labels = processLabels(currentUser.getId(), request.labelNames());
            userBoardGame.setLabels(labels);
        }
        
        UserBoardGame savedGame = userBoardGameRepository.save(userBoardGame);
        return GameCollectionItemDto.from(savedGame);
    }

    @Transactional
    public void deleteCurrentUserAccount() {
        User currentUser = userContextService.getCurrentUser()
            .orElseThrow(InvalidCredentialsException::new);
        
        userRepository.delete(currentUser);
    }
}
