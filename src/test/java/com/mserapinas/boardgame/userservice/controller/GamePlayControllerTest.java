package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.CreateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.response.GamePlayDto;
import com.mserapinas.boardgame.userservice.exception.InvalidWinnerException;
import com.mserapinas.boardgame.userservice.service.GamePlayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("DataFlowIssue")
@WebMvcTest(controllers = GamePlayController.class)
@AutoConfigureMockMvc(addFilters = false)
class GamePlayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GamePlayService gamePlayService;

    private static final String BASE_URL = "/api/v1/plays";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PLAY_ID = 42L;
    private static final Integer TEST_GAME_ID = 1001;

    private GamePlayDto sampleDto() {
        GamePlayDto.PlayerRef kipras = new GamePlayDto.PlayerRef(2L, "Kipras");
        return new GamePlayDto(
            TEST_PLAY_ID,
            TEST_USER_ID,
            TEST_GAME_ID,
            LocalDate.of(2026, 7, 6),
            2,
            90,
            List.of(kipras),
            Arrays.asList(kipras, null),
            "Fun session",
            OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create game play successfully")
    void shouldCreateGamePlaySuccessfully() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            TEST_GAME_ID, LocalDate.of(2026, 7, 6), 2, 90, Set.of(2L), Arrays.asList(2L, null), "Fun session"
        );

        when(gamePlayService.createGamePlay(eq(TEST_USER_ID), any(CreateGamePlayRequest.class)))
            .thenReturn(sampleDto());

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_PLAY_ID))
                .andExpect(jsonPath("$.gameId").value(TEST_GAME_ID))
                .andExpect(jsonPath("$.timesPlayed").value(2))
                .andExpect(jsonPath("$.winners[0].id").value(2))
                .andExpect(jsonPath("$.winners[1]").doesNotExist());

        verify(gamePlayService).createGamePlay(eq(TEST_USER_ID), any(CreateGamePlayRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when playedAt is missing")
    void shouldRejectMissingPlayedAt() throws Exception {
        CreateGamePlayRequest invalid = new CreateGamePlayRequest(
            TEST_GAME_ID, null, 1, null, null, List.of(), null
        );

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(gamePlayService, never()).createGamePlay(any(), any());
    }

    @Test
    @DisplayName("Should return bad request when timesPlayed is less than 1")
    void shouldRejectTimesPlayedBelowMinimum() throws Exception {
        CreateGamePlayRequest invalid = new CreateGamePlayRequest(
            TEST_GAME_ID, LocalDate.now(), 0, null, null, List.of(), null
        );

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(gamePlayService, never()).createGamePlay(any(), any());
    }

    @Test
    @DisplayName("Should return bad request when winner not among players")
    void shouldReturnBadRequestWhenInvalidWinner() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            TEST_GAME_ID, LocalDate.now(), 1, null, Set.of(2L), List.of(999L), null
        );

        when(gamePlayService.createGamePlay(eq(TEST_USER_ID), any(CreateGamePlayRequest.class)))
            .thenThrow(new InvalidWinnerException("Winner must be one of the players who played"));

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should list plays for a game")
    void shouldListPlaysForGame() throws Exception {
        when(gamePlayService.getPlaysForGame(TEST_USER_ID, TEST_GAME_ID))
            .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .param("gameId", TEST_GAME_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(TEST_PLAY_ID));

        verify(gamePlayService).getPlaysForGame(TEST_USER_ID, TEST_GAME_ID);
    }
}
