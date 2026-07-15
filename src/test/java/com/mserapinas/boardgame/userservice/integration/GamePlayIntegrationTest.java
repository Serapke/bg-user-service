package com.mserapinas.boardgame.userservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.CreateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("DataFlowIssue")
@AutoConfigureMockMvc
class GamePlayIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PLAYS_URL = "/api/v1/plays";
    private static final String AUTH_URL = "/api/v1/auth";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Integer GAME_ID = 1001;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        userId1 = register("player1@example.com", "Player One");
        userId2 = register("player2@example.com", "Player Two");
    }

    private Long register(String email, String name) throws Exception {
        RegisterRequest req = new RegisterRequest(email, name, "Password123!");
        MvcResult result = mockMvc.perform(post(AUTH_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    @DisplayName("Should create a game play with players and per-game winners")
    @Transactional
    void shouldCreateGamePlayWithPlayersAndWinners() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            GAME_ID, LocalDate.of(2026, 7, 6), 2, 90, Set.of(userId2), List.of(List.of(userId2), List.of()), "Rematch"
        );

        mockMvc.perform(post(PLAYS_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.loggerId").value(userId1))
                .andExpect(jsonPath("$.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.timesPlayed").value(2))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.winners[0][0].id").value(userId2))
                .andExpect(jsonPath("$.winners[1]").isEmpty())
                .andExpect(jsonPath("$.players[0].id").value(userId2));
    }

    @Test
    @DisplayName("Should reject winner who is not among players")
    @Transactional
    void shouldRejectInvalidWinner() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            GAME_ID, LocalDate.now(), 1, null, Set.of(userId2), List.of(List.of(99999L)), null
        );

        mockMvc.perform(post(PLAYS_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject winner count that does not match times played")
    @Transactional
    void shouldRejectWinnerCountMismatch() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            GAME_ID, LocalDate.now(), 3, null, Set.of(userId2), List.of(List.of(userId2)), null
        );

        mockMvc.perform(post(PLAYS_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should list plays for a game filtered to the logging user")
    @Transactional
    void shouldListPlaysForGame() throws Exception {
        CreateGamePlayRequest request = new CreateGamePlayRequest(
            GAME_ID, LocalDate.now(), 1, null, null, List.of(), null
        );

        mockMvc.perform(post(PLAYS_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PLAYS_URL)
                .header(USER_ID_HEADER, userId1)
                .param("gameId", GAME_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].gameId").value(GAME_ID));

        mockMvc.perform(get(PLAYS_URL)
                .header(USER_ID_HEADER, userId2)
                .param("gameId", GAME_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
