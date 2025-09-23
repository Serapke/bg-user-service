package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.AddGameToCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGameCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionDto;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionItemDto;
import com.mserapinas.boardgame.userservice.dto.response.LabelDto;
import com.mserapinas.boardgame.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CollectionController.class, com.mserapinas.boardgame.userservice.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private com.mserapinas.boardgame.userservice.service.JwtService jwtService;

    @MockitoBean
    private com.mserapinas.boardgame.userservice.repository.UserRepository userRepository;

    @MockitoBean
    private com.mserapinas.boardgame.userservice.service.UserContextService userContextService;

    @MockitoBean
    private com.mserapinas.boardgame.userservice.repository.UserBoardGameRepository userBoardGameRepository;

    @MockitoBean
    private com.mserapinas.boardgame.userservice.repository.LabelRepository labelRepository;

    private static final String BASE_URL = "/api/v1/collection";

    @Test
    @DisplayName("Should get current user game collection successfully")
    void shouldGetCurrentUserGameCollectionSuccessfully() throws Exception {
        LabelDto labelDto = new LabelDto(1L, "Strategy");
        GameCollectionItemDto gameItem = new GameCollectionItemDto(
            1L, 1001, "Great game", OffsetDateTime.now(), Set.of(labelDto)
        );
        GameCollectionDto collection = new GameCollectionDto(List.of(gameItem));
        
        when(userService.getCurrentUserGameCollection()).thenReturn(collection);
        
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games[0].id").value(1L))
                .andExpect(jsonPath("$.games[0].gameId").value(1001))
                .andExpect(jsonPath("$.games[0].notes").value("Great game"))
                .andExpect(jsonPath("$.games[0].labels[0].id").value(1L))
                .andExpect(jsonPath("$.games[0].labels[0].name").value("Strategy"));
        
        verify(userService).getCurrentUserGameCollection();
    }

    @Test
    @DisplayName("Should return unauthorized when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        when(userService.getCurrentUserGameCollection())
            .thenThrow(new com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException());
        
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
        
        verify(userService).getCurrentUserGameCollection();
    }

    @Test
    @DisplayName("Should add game to collection successfully")
    void shouldAddGameToCollectionSuccessfully() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1001, "New game notes", Set.of("Strategy", "Family")
        );
        
        LabelDto labelDto = new LabelDto(1L, "Strategy");
        GameCollectionItemDto responseItem = new GameCollectionItemDto(
            1L, 1001, "New game notes", OffsetDateTime.now(), Set.of(labelDto)
        );
        
        when(userService.addGameToCollection(any(AddGameToCollectionRequest.class))).thenReturn(responseItem);
        
        mockMvc.perform(post(BASE_URL + "/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.gameId").value(1001))
                .andExpect(jsonPath("$.notes").value("New game notes"))
                .andExpect(jsonPath("$.labels[0].name").value("Strategy"));
        
        verify(userService).addGameToCollection(any(AddGameToCollectionRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when adding game with invalid data")
    void shouldReturnBadRequestWhenAddingGameWithInvalidData() throws Exception {
        AddGameToCollectionRequest invalidRequest = new AddGameToCollectionRequest(
            null, "Notes", Set.of() // gameId is null
        );
        
        mockMvc.perform(post(BASE_URL + "/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).addGameToCollection(any(AddGameToCollectionRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when adding game with negative gameId")
    void shouldReturnBadRequestWhenAddingGameWithNegativeGameId() throws Exception {
        AddGameToCollectionRequest invalidRequest = new AddGameToCollectionRequest(
            -1, "Notes", Set.of()
        );
        
        mockMvc.perform(post(BASE_URL + "/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).addGameToCollection(any(AddGameToCollectionRequest.class));
    }

    @Test
    @DisplayName("Should update game in collection successfully")
    void shouldUpdateGameInCollectionSuccessfully() throws Exception {
        Integer gameId = 1001;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest(
            "Updated notes", Set.of("Updated", "Strategy")
        );
        
        LabelDto labelDto = new LabelDto(1L, "Updated");
        GameCollectionItemDto responseItem = new GameCollectionItemDto(
            1L, gameId, "Updated notes", OffsetDateTime.now(), Set.of(labelDto)
        );
        
        when(userService.updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class)))
            .thenReturn(responseItem);
        
        mockMvc.perform(put(BASE_URL + "/games/" + gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.labels[0].name").value("Updated"));
        
        verify(userService).updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating game with notes too long")
    void shouldReturnBadRequestWhenUpdatingGameWithNotesTooLong() throws Exception {
        Integer gameId = 1001;
        String longNotes = "a".repeat(1001); // Exceeds 1000 character limit
        UpdateGameCollectionRequest invalidRequest = new UpdateGameCollectionRequest(
            longNotes, Set.of()
        );
        
        mockMvc.perform(put(BASE_URL + "/games/" + gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateGameInCollection(anyInt(), any(UpdateGameCollectionRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating game with too many labels")
    void shouldReturnBadRequestWhenUpdatingGameWithTooManyLabels() throws Exception {
        Integer gameId = 1001;
        Set<String> tooManyLabels = Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"); // Exceeds 10 label limit
        UpdateGameCollectionRequest invalidRequest = new UpdateGameCollectionRequest(
            "Notes", tooManyLabels
        );
        
        mockMvc.perform(put(BASE_URL + "/games/" + gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateGameInCollection(anyInt(), any(UpdateGameCollectionRequest.class));
    }

    @Test
    @DisplayName("Should delete game from collection successfully")
    void shouldDeleteGameFromCollectionSuccessfully() throws Exception {
        Integer gameId = 1001;
        
        doNothing().when(userService).deleteGameFromCollection(gameId);
        
        mockMvc.perform(delete(BASE_URL + "/games/" + gameId))
                .andExpect(status().isNoContent());
        
        verify(userService).deleteGameFromCollection(gameId);
    }

    @Test
    @DisplayName("Should return bad request when service throws IllegalArgumentException")
    void shouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
        Integer gameId = 1001;
        
        doThrow(new IllegalArgumentException("Game not found in your collection"))
            .when(userService).deleteGameFromCollection(gameId);
        
        mockMvc.perform(delete(BASE_URL + "/games/" + gameId))
                .andExpect(status().isBadRequest());
        
        verify(userService).deleteGameFromCollection(gameId);
    }

    @Test
    @DisplayName("Should handle duplicate game addition gracefully")
    void shouldHandleDuplicateGameAdditionGracefully() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1001, "Duplicate game", Set.of()
        );
        
        when(userService.addGameToCollection(any(AddGameToCollectionRequest.class)))
            .thenThrow(new IllegalArgumentException("Game already exists in your collection"));
        
        mockMvc.perform(post(BASE_URL + "/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(userService).addGameToCollection(any(AddGameToCollectionRequest.class));
    }

    @Test
    @DisplayName("Should handle update non-existent game gracefully")
    void shouldHandleUpdateNonExistentGameGracefully() throws Exception {
        Integer gameId = 9999;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest(
            "Updated notes", Set.of()
        );
        
        when(userService.updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class)))
            .thenThrow(new IllegalArgumentException("Game not found in your collection"));
        
        mockMvc.perform(put(BASE_URL + "/games/" + gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(userService).updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class));
    }

    @Test
    @DisplayName("Should accept valid request with empty optional fields")
    void shouldAcceptValidRequestWithEmptyOptionalFields() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1001, null, null // notes and labelNames are optional
        );
        
        LabelDto labelDto = new LabelDto(1L, "Default");
        GameCollectionItemDto responseItem = new GameCollectionItemDto(
            1L, 1001, null, OffsetDateTime.now(), Set.of(labelDto)
        );
        
        when(userService.addGameToCollection(any(AddGameToCollectionRequest.class))).thenReturn(responseItem);
        
        mockMvc.perform(post(BASE_URL + "/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        verify(userService).addGameToCollection(any(AddGameToCollectionRequest.class));
    }

    @Test
    @DisplayName("Should accept valid update request with empty optional fields")
    void shouldAcceptValidUpdateRequestWithEmptyOptionalFields() throws Exception {
        Integer gameId = 1001;
        UpdateGameCollectionRequest request = new UpdateGameCollectionRequest(
            null, null // both fields are optional
        );
        
        GameCollectionItemDto responseItem = new GameCollectionItemDto(
            1L, gameId, null, OffsetDateTime.now(), Set.of()
        );
        
        when(userService.updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class)))
            .thenReturn(responseItem);
        
        mockMvc.perform(put(BASE_URL + "/games/" + gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        verify(userService).updateGameInCollection(eq(gameId), any(UpdateGameCollectionRequest.class));
    }
}