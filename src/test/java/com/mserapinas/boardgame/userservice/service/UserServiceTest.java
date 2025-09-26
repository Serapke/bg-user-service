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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBoardGameRepository userBoardGameRepository;

    @Mock
    private LabelRepository labelRepository;

    private UserService userService;

    private User testUser;
    private UserBoardGame testUserBoardGame;
    private Label testLabel;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userBoardGameRepository, labelRepository);

        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setCreatedAt(OffsetDateTime.now());

        testLabel = new Label(1L, "Strategy");
        testLabel.setId(1L);

        testUserBoardGame = new UserBoardGame(TEST_USER_ID, 1001, "Test notes");
        testUserBoardGame.setId(1L);
        testUserBoardGame.setLabels(Set.of(testLabel));
        testUserBoardGame.setModifiedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserProfileSuccessfully() {
        String newName = "Updated Name";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(newName);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUserProfile(TEST_USER_ID, request);

        assertNotNull(result);
        assertEquals(newName, testUser.getName());
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when updating profile for non-existent user")
    void shouldThrowExceptionWhenUpdatingProfileForNonExistentUser() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
            () -> userService.updateUserProfile(TEST_USER_ID, request));

        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user game collection successfully")
    void shouldGetUserGameCollectionSuccessfully() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.findByUserIdWithLabels(TEST_USER_ID)).thenReturn(List.of(testUserBoardGame));

        GameCollectionDto result = userService.getUserGameCollection(TEST_USER_ID);

        assertNotNull(result);
        assertEquals(1, result.games().size());
        assertEquals(1001, result.games().getFirst().gameId());
        assertEquals("Test notes", result.games().getFirst().notes());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).findByUserIdWithLabels(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when getting collection for non-existent user")
    void shouldThrowExceptionWhenGettingCollectionForNonExistentUser() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
            () -> userService.getUserGameCollection(TEST_USER_ID));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository, never()).findByUserIdWithLabels(any());
    }

    @Test
    @DisplayName("Should add game to collection successfully")
    void shouldAddGameToCollectionSuccessfully() {
        Integer gameId = 1002;
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(gameId, "New game", Set.of("Action"));

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.existsByUserIdAndGameId(TEST_USER_ID, gameId)).thenReturn(false);
        when(labelRepository.findByUserIdAndNameIn(TEST_USER_ID, request.labelNames())).thenReturn(List.of());
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);

        GameCollectionItemDto result = userService.addGameToCollection(TEST_USER_ID, request);

        assertNotNull(result);
        assertEquals(1L, result.id());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).existsByUserIdAndGameId(TEST_USER_ID, gameId);
        verify(userBoardGameRepository).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should throw exception when adding game to collection for non-existent user")
    void shouldThrowExceptionWhenAddingGameToCollectionForNonExistentUser() {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(1002, "New game", Set.of());

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
            () -> userService.addGameToCollection(TEST_USER_ID, request));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository, never()).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate game")
    void shouldThrowExceptionWhenAddingDuplicateGame() {
        Integer gameId = 1001;
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(gameId, "Duplicate", Set.of());

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.existsByUserIdAndGameId(TEST_USER_ID, gameId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> userService.addGameToCollection(TEST_USER_ID, request));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).existsByUserIdAndGameId(TEST_USER_ID, gameId);
        verify(userBoardGameRepository, never()).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should update game in collection successfully")
    void shouldUpdateGameInCollectionSuccessfully() {
        Integer gameId = 1001;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest("Updated notes", Set.of("Strategy"));

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.findByUserIdAndGameIdWithLabels(TEST_USER_ID, gameId)).thenReturn(Optional.of(testUserBoardGame));
        when(labelRepository.findByUserIdAndNameIn(TEST_USER_ID, request.labelNames())).thenReturn(List.of(testLabel));
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);

        GameCollectionItemDto result = userService.updateGameInCollection(TEST_USER_ID, gameId, request);

        assertNotNull(result);
        assertEquals("Updated notes", testUserBoardGame.getNotes());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).findByUserIdAndGameIdWithLabels(TEST_USER_ID, gameId);
        verify(userBoardGameRepository).save(testUserBoardGame);
    }

    @Test
    @DisplayName("Should throw exception when updating game for non-existent user")
    void shouldThrowExceptionWhenUpdatingGameForNonExistentUser() {
        Integer gameId = 1001;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest("Updated", Set.of());

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
            () -> userService.updateGameInCollection(TEST_USER_ID, gameId, request));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository, never()).findByUserIdAndGameIdWithLabels(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent game")
    void shouldThrowExceptionWhenUpdatingNonExistentGame() {
        Integer gameId = 9999;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest("Updated", Set.of());

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.findByUserIdAndGameIdWithLabels(TEST_USER_ID, gameId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> userService.updateGameInCollection(TEST_USER_ID, gameId, request));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).findByUserIdAndGameIdWithLabels(TEST_USER_ID, gameId);
        verify(userBoardGameRepository, never()).save(any(UserBoardGame.class));
    }

    @Test
    @DisplayName("Should delete game from collection successfully")
    void shouldDeleteGameFromCollectionSuccessfully() {
        Integer gameId = 1001;

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.existsByUserIdAndGameId(TEST_USER_ID, gameId)).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteGameFromCollection(TEST_USER_ID, gameId));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).existsByUserIdAndGameId(TEST_USER_ID, gameId);
        verify(userBoardGameRepository).deleteByUserIdAndGameId(TEST_USER_ID, gameId);
    }

    @Test
    @DisplayName("Should throw exception when deleting game for non-existent user")
    void shouldThrowExceptionWhenDeletingGameForNonExistentUser() {
        Integer gameId = 1001;

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
            () -> userService.deleteGameFromCollection(TEST_USER_ID, gameId));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository, never()).deleteByUserIdAndGameId(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent game")
    void shouldThrowExceptionWhenDeletingNonExistentGame() {
        Integer gameId = 9999;

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.existsByUserIdAndGameId(TEST_USER_ID, gameId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> userService.deleteGameFromCollection(TEST_USER_ID, gameId));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).existsByUserIdAndGameId(TEST_USER_ID, gameId);
        verify(userBoardGameRepository, never()).deleteByUserIdAndGameId(any(), any());
    }

    @Test
    @DisplayName("Should delete user account successfully")
    void shouldDeleteUserAccountSuccessfully() {
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userService.deleteUserAccount(TEST_USER_ID));

        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user account")
    void shouldThrowExceptionWhenDeletingNonExistentUserAccount() {
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
            () -> userService.deleteUserAccount(TEST_USER_ID));

        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should handle empty game collection")
    void shouldHandleEmptyGameCollection() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.findByUserIdWithLabels(TEST_USER_ID)).thenReturn(List.of());

        GameCollectionDto result = userService.getUserGameCollection(TEST_USER_ID);

        assertNotNull(result);
        assertTrue(result.games().isEmpty());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).findByUserIdWithLabels(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle adding game with no labels")
    void shouldHandleAddingGameWithNoLabels() {
        Integer gameId = 1002;
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(gameId, "No labels game", null);

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.existsByUserIdAndGameId(TEST_USER_ID, gameId)).thenReturn(false);
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);

        GameCollectionItemDto result = userService.addGameToCollection(TEST_USER_ID, request);

        assertNotNull(result);

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).save(any(UserBoardGame.class));
        verify(labelRepository, never()).findByUserIdAndNameIn(any(), any());
    }

    @Test
    @DisplayName("Should handle updating game with null labels")
    void shouldHandleUpdatingGameWithNullLabels() {
        Integer gameId = 1001;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest("Updated notes", null);

        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(userBoardGameRepository.findByUserIdAndGameIdWithLabels(TEST_USER_ID, gameId)).thenReturn(Optional.of(testUserBoardGame));
        when(userBoardGameRepository.save(any(UserBoardGame.class))).thenReturn(testUserBoardGame);

        GameCollectionItemDto result = userService.updateGameInCollection(TEST_USER_ID, gameId, request);

        assertNotNull(result);
        assertEquals("Updated notes", testUserBoardGame.getNotes());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(userBoardGameRepository).save(testUserBoardGame);
        verify(labelRepository, never()).findByUserIdAndNameIn(any(), any());
    }
}