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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserBoardGameRepository userBoardGameRepository;
    
    @Mock
    private LabelRepository labelRepository;
    
    @Mock
    private UserContextService userContextService;
    
    private UserService userService;
    
    private User testUser;
    private UserBoardGame testUserBoardGame;
    private Label testLabel;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userBoardGameRepository, labelRepository, userContextService);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setCreatedAt(OffsetDateTime.now());
        
        testLabel = new Label(1L, "Strategy");
        testLabel.setId(1L);
        
        testUserBoardGame = new UserBoardGame(1L, 1001, "Test notes");
        testUserBoardGame.setId(1L);
        testUserBoardGame.setLabels(Set.of(testLabel));
        testUserBoardGame.setModifiedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should update current user profile successfully")
    void shouldUpdateCurrentUserProfileSuccessfully() {
        String newName = "Updated Name";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(newName);
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        User result = userService.updateCurrentUserProfile(request);
        
        assertNotNull(result);
        assertEquals(newName, testUser.getName());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when updating profile without authenticated user")
    void shouldThrowExceptionWhenUpdatingProfileWithoutAuthenticatedUser() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.empty());
        
        assertThrows(InvalidCredentialsException.class, 
            () -> userService.updateCurrentUserProfile(request));
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get current user game collection successfully")
    void shouldGetCurrentUserGameCollectionSuccessfully() {
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.findByUserIdWithLabels(testUser.getId()))
            .thenReturn(List.of(testUserBoardGame));
        
        GameCollectionDto result = userService.getCurrentUserGameCollection();
        
        assertNotNull(result);
        assertNotNull(result.games());
        assertEquals(1, result.games().size());
        
        GameCollectionItemDto gameItem = result.games().get(0);
        assertEquals(testUserBoardGame.getId(), gameItem.id());
        assertEquals(testUserBoardGame.getGameId(), gameItem.gameId());
        assertEquals(testUserBoardGame.getNotes(), gameItem.notes());
    }

    @Test
    @DisplayName("Should throw exception when getting collection without authenticated user")
    void shouldThrowExceptionWhenGettingCollectionWithoutAuthenticatedUser() {
        when(userContextService.getCurrentUser()).thenReturn(Optional.empty());
        
        assertThrows(InvalidCredentialsException.class, 
            () -> userService.getCurrentUserGameCollection());
    }

    @Test
    @DisplayName("Should add game to collection successfully")
    void shouldAddGameToCollectionSuccessfully() {
        Integer gameId = 1002;
        String notes = "New game notes";
        Set<String> labelNames = Set.of("Strategy", "New");
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(gameId, notes, labelNames);
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.existsByUserIdAndGameId(testUser.getId(), gameId)).thenReturn(false);
        when(labelRepository.findByUserIdAndNameIn(testUser.getId(), labelNames))
            .thenReturn(List.of(testLabel));
        when(labelRepository.save(any(Label.class))).thenReturn(new Label(1L, "New"));
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);
        
        GameCollectionItemDto result = userService.addGameToCollection(request);
        
        assertNotNull(result);
        verify(userBoardGameRepository).existsByUserIdAndGameId(testUser.getId(), gameId);
        verify(userBoardGameRepository).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate game to collection")
    void shouldThrowExceptionWhenAddingDuplicateGameToCollection() {
        Integer gameId = 1001;
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(gameId, "notes", Set.of());
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.existsByUserIdAndGameId(testUser.getId(), gameId)).thenReturn(true);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.addGameToCollection(request));
        
        assertEquals("Game already exists in your collection", exception.getMessage());
        verify(userBoardGameRepository, never()).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should update game in collection successfully")
    void shouldUpdateGameInCollectionSuccessfully() {
        Integer gameId = 1001;
        String updatedNotes = "Updated notes";
        Set<String> labelNames = Set.of("Updated");
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest(updatedNotes, labelNames);
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.findByUserIdAndGameIdWithLabels(testUser.getId(), gameId))
            .thenReturn(Optional.of(testUserBoardGame));
        when(labelRepository.findByUserIdAndNameIn(testUser.getId(), labelNames))
            .thenReturn(List.of());
        when(labelRepository.save(any(Label.class))).thenReturn(new Label(1L, "Updated"));
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);
        
        GameCollectionItemDto result = userService.updateGameInCollection(gameId, request);
        
        assertNotNull(result);
        assertEquals(updatedNotes, testUserBoardGame.getNotes());
        verify(userBoardGameRepository).save(testUserBoardGame);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent game")
    void shouldThrowExceptionWhenUpdatingNonExistentGame() {
        Integer gameId = 9999;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest("notes", Set.of());
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.findByUserIdAndGameIdWithLabels(testUser.getId(), gameId))
            .thenReturn(Optional.empty());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.updateGameInCollection(gameId, request));
        
        assertEquals("Game not found in your collection", exception.getMessage());
    }

    @Test
    @DisplayName("Should delete game from collection successfully")
    void shouldDeleteGameFromCollectionSuccessfully() {
        Integer gameId = 1001;
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.existsByUserIdAndGameId(testUser.getId(), gameId)).thenReturn(true);
        
        assertDoesNotThrow(() -> userService.deleteGameFromCollection(gameId));
        
        verify(userBoardGameRepository).deleteByUserIdAndGameId(testUser.getId(), gameId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent game from collection")
    void shouldThrowExceptionWhenDeletingNonExistentGameFromCollection() {
        Integer gameId = 9999;
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.existsByUserIdAndGameId(testUser.getId(), gameId)).thenReturn(false);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.deleteGameFromCollection(gameId));
        
        assertEquals("Game not found in your collection", exception.getMessage());
        verify(userBoardGameRepository, never()).deleteByUserIdAndGameId(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should delete current user account successfully")
    void shouldDeleteCurrentUserAccountSuccessfully() {
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        
        assertDoesNotThrow(() -> userService.deleteCurrentUserAccount());
        
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Should throw exception when deleting account without authenticated user")
    void shouldThrowExceptionWhenDeletingAccountWithoutAuthenticatedUser() {
        when(userContextService.getCurrentUser()).thenReturn(Optional.empty());
        
        assertThrows(InvalidCredentialsException.class, 
            () -> userService.deleteCurrentUserAccount());
        
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should process labels correctly with existing and new labels")
    void shouldProcessLabelsCorrectlyWithExistingAndNewLabels() {
        Long userId = 1L;
        Set<String> labelNames = Set.of("Existing", "New");
        Label existingLabel = new Label(userId, "Existing");
        
        when(labelRepository.findByUserIdAndNameIn(userId, labelNames))
            .thenReturn(List.of(existingLabel));
        when(labelRepository.save(any(Label.class))).thenReturn(new Label(userId, "New"));
        
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(1001, "notes", labelNames);
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(userBoardGameRepository.existsByUserIdAndGameId(userId, 1001)).thenReturn(false);
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);
        
        userService.addGameToCollection(request);
        
        verify(labelRepository).findByUserIdAndNameIn(userId, labelNames);
        verify(labelRepository).save(any(Label.class)); // For the new label
    }
}