package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.dto.response.FriendListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendRequestListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendSuggestionDto;
import com.mserapinas.boardgame.userservice.exception.*;
import com.mserapinas.boardgame.userservice.service.FriendshipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FriendshipController.class)
@AutoConfigureMockMvc(addFilters = false)
class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendshipService friendshipService;

    private static final String BASE_URL = "/api/v1/friendships";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Long TEST_USER_ID = 1L;
    private static final Long FRIEND_USER_ID = 2L;

    // ========== Send Friend Request Tests ==========

    @Test
    @DisplayName("Should send friend request successfully")
    void shouldSendFriendRequestSuccessfully() throws Exception {
        doNothing().when(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isCreated());

        verify(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when sending friend request without X-User-ID header")
    void shouldReturnBadRequestWhenSendingFriendRequestWithoutHeader() throws Exception {
        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", FRIEND_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).sendFriendRequest(any(), any());
    }

    @Test
    @DisplayName("Should return bad request when trying to friend yourself")
    void shouldReturnBadRequestWhenTryingToFriendYourself() throws Exception {
        doThrow(new SelfFriendshipException()).when(friendshipService)
                .sendFriendRequest(TEST_USER_ID, TEST_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", TEST_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService).sendFriendRequest(TEST_USER_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return conflict when already friends")
    void shouldReturnConflictWhenAlreadyFriends() throws Exception {
        doThrow(new AlreadyFriendsException(TEST_USER_ID, FRIEND_USER_ID))
                .when(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isConflict());

        verify(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return conflict when friend request already sent")
    void shouldReturnConflictWhenFriendRequestAlreadySent() throws Exception {
        doThrow(new FriendRequestAlreadySentException(TEST_USER_ID, FRIEND_USER_ID))
                .when(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isConflict());

        verify(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return not found when friend user does not exist")
    void shouldReturnNotFoundWhenFriendUserDoesNotExist() throws Exception {
        doThrow(new UserNotFoundException(FRIEND_USER_ID))
                .when(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).sendFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    // ========== Accept Friend Request Tests ==========

    @Test
    @DisplayName("Should accept friend request successfully")
    void shouldAcceptFriendRequestSuccessfully() throws Exception {
        doNothing().when(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{requesterUserId}/accept", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk());

        verify(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when accepting friend request without X-User-ID header")
    void shouldReturnBadRequestWhenAcceptingFriendRequestWithoutHeader() throws Exception {
        mockMvc.perform(post(BASE_URL + "/requests/{requesterUserId}/accept", FRIEND_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).acceptFriendRequest(any(), any());
    }

    @Test
    @DisplayName("Should return not found when accepting non-existent friend request")
    void shouldReturnNotFoundWhenAcceptingNonExistentFriendRequest() throws Exception {
        doThrow(new FriendRequestNotFoundException(FRIEND_USER_ID, TEST_USER_ID))
                .when(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{requesterUserId}/accept", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return conflict when accepting request from already friend")
    void shouldReturnConflictWhenAcceptingRequestFromAlreadyFriend() throws Exception {
        doThrow(new AlreadyFriendsException(TEST_USER_ID, FRIEND_USER_ID))
                .when(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(post(BASE_URL + "/requests/{requesterUserId}/accept", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isConflict());

        verify(friendshipService).acceptFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    // ========== Decline Friend Request Tests ==========

    @Test
    @DisplayName("Should decline friend request successfully")
    void shouldDeclineFriendRequestSuccessfully() throws Exception {
        doNothing().when(friendshipService).declineFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/requests/{requesterUserId}/decline", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());

        verify(friendshipService).declineFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when declining friend request without X-User-ID header")
    void shouldReturnBadRequestWhenDecliningFriendRequestWithoutHeader() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/requests/{requesterUserId}/decline", FRIEND_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).declineFriendRequest(any(), any());
    }

    @Test
    @DisplayName("Should return not found when declining non-existent friend request")
    void shouldReturnNotFoundWhenDecliningNonExistentFriendRequest() throws Exception {
        doThrow(new FriendRequestNotFoundException(FRIEND_USER_ID, TEST_USER_ID))
                .when(friendshipService).declineFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/requests/{requesterUserId}/decline", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).declineFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    // ========== Cancel Friend Request Tests ==========

    @Test
    @DisplayName("Should cancel friend request successfully")
    void shouldCancelFriendRequestSuccessfully() throws Exception {
        doNothing().when(friendshipService).cancelFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/requests/{friendUserId}/cancel", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());

        verify(friendshipService).cancelFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when canceling friend request without X-User-ID header")
    void shouldReturnBadRequestWhenCancelingFriendRequestWithoutHeader() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/requests/{friendUserId}/cancel", FRIEND_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).cancelFriendRequest(any(), any());
    }

    @Test
    @DisplayName("Should return not found when canceling non-existent friend request")
    void shouldReturnNotFoundWhenCancelingNonExistentFriendRequest() throws Exception {
        doThrow(new FriendRequestNotFoundException(TEST_USER_ID, FRIEND_USER_ID))
                .when(friendshipService).cancelFriendRequest(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/requests/{friendUserId}/cancel", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).cancelFriendRequest(TEST_USER_ID, FRIEND_USER_ID);
    }

    // ========== Get Incoming Requests Tests ==========

    @Test
    @DisplayName("Should get incoming friend requests successfully")
    void shouldGetIncomingFriendRequestsSuccessfully() throws Exception {
        FriendRequestListDto requestListDto = new FriendRequestListDto(Collections.emptyList(), 0L);
        when(friendshipService.getIncomingRequests(TEST_USER_ID)).thenReturn(requestListDto);

        mockMvc.perform(get(BASE_URL + "/requests/incoming")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(friendshipService).getIncomingRequests(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when getting incoming requests without X-User-ID header")
    void shouldReturnBadRequestWhenGettingIncomingRequestsWithoutHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/requests/incoming"))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).getIncomingRequests(any());
    }

    @Test
    @DisplayName("Should return not found when getting incoming requests for non-existent user")
    void shouldReturnNotFoundWhenGettingIncomingRequestsForNonExistentUser() throws Exception {
        when(friendshipService.getIncomingRequests(TEST_USER_ID))
                .thenThrow(new UserNotFoundException(TEST_USER_ID));

        mockMvc.perform(get(BASE_URL + "/requests/incoming")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).getIncomingRequests(TEST_USER_ID);
    }

    // ========== Get Outgoing Requests Tests ==========

    @Test
    @DisplayName("Should get outgoing friend requests successfully")
    void shouldGetOutgoingFriendRequestsSuccessfully() throws Exception {
        FriendRequestListDto requestListDto = new FriendRequestListDto(Collections.emptyList(), 0L);
        when(friendshipService.getOutgoingRequests(TEST_USER_ID)).thenReturn(requestListDto);

        mockMvc.perform(get(BASE_URL + "/requests/outgoing")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(friendshipService).getOutgoingRequests(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when getting outgoing requests without X-User-ID header")
    void shouldReturnBadRequestWhenGettingOutgoingRequestsWithoutHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/requests/outgoing"))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).getOutgoingRequests(any());
    }

    @Test
    @DisplayName("Should return not found when getting outgoing requests for non-existent user")
    void shouldReturnNotFoundWhenGettingOutgoingRequestsForNonExistentUser() throws Exception {
        when(friendshipService.getOutgoingRequests(TEST_USER_ID))
                .thenThrow(new UserNotFoundException(TEST_USER_ID));

        mockMvc.perform(get(BASE_URL + "/requests/outgoing")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).getOutgoingRequests(TEST_USER_ID);
    }

    // ========== Get Friends Tests ==========

    @Test
    @DisplayName("Should get friends list successfully")
    void shouldGetFriendsListSuccessfully() throws Exception {
        FriendListDto friendListDto = new FriendListDto(Collections.emptyList(), 0L);
        when(friendshipService.getFriends(TEST_USER_ID)).thenReturn(friendListDto);

        mockMvc.perform(get(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(friendshipService).getFriends(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when getting friends without X-User-ID header")
    void shouldReturnBadRequestWhenGettingFriendsWithoutHeader() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).getFriends(any());
    }

    @Test
    @DisplayName("Should return not found when getting friends for non-existent user")
    void shouldReturnNotFoundWhenGettingFriendsForNonExistentUser() throws Exception {
        when(friendshipService.getFriends(TEST_USER_ID))
                .thenThrow(new UserNotFoundException(TEST_USER_ID));

        mockMvc.perform(get(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).getFriends(TEST_USER_ID);
    }

    // ========== Remove Friend Tests ==========

    @Test
    @DisplayName("Should remove friend successfully")
    void shouldRemoveFriendSuccessfully() throws Exception {
        doNothing().when(friendshipService).removeFriend(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());

        verify(friendshipService).removeFriend(TEST_USER_ID, FRIEND_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when removing friend without X-User-ID header")
    void shouldReturnBadRequestWhenRemovingFriendWithoutHeader() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{friendUserId}", FRIEND_USER_ID))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).removeFriend(any(), any());
    }

    @Test
    @DisplayName("Should return not found when removing non-existent friend")
    void shouldReturnNotFoundWhenRemovingNonExistentFriend() throws Exception {
        doThrow(new FriendRequestNotFoundException(TEST_USER_ID, FRIEND_USER_ID))
                .when(friendshipService).removeFriend(TEST_USER_ID, FRIEND_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/{friendUserId}", FRIEND_USER_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).removeFriend(TEST_USER_ID, FRIEND_USER_ID);
    }

    // ========== Get Friend Suggestions Tests ==========

    @Test
    @DisplayName("Should get friend suggestions successfully")
    void shouldGetFriendSuggestionsSuccessfully() throws Exception {
        FriendSuggestionDto suggestionDto = new FriendSuggestionDto(Collections.emptyList(), 0L);
        when(friendshipService.getFriendSuggestions(TEST_USER_ID)).thenReturn(suggestionDto);

        mockMvc.perform(get(BASE_URL + "/suggestions")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(friendshipService).getFriendSuggestions(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when getting friend suggestions without X-User-ID header")
    void shouldReturnBadRequestWhenGettingFriendSuggestionsWithoutHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/suggestions"))
                .andExpect(status().isBadRequest());

        verify(friendshipService, never()).getFriendSuggestions(any());
    }

    @Test
    @DisplayName("Should return not found when getting friend suggestions for non-existent user")
    void shouldReturnNotFoundWhenGettingFriendSuggestionsForNonExistentUser() throws Exception {
        when(friendshipService.getFriendSuggestions(TEST_USER_ID))
                .thenThrow(new UserNotFoundException(TEST_USER_ID));

        mockMvc.perform(get(BASE_URL + "/suggestions")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(friendshipService).getFriendSuggestions(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle invalid user ID format in header")
    void shouldHandleInvalidUserIdFormatInHeader() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header(USER_ID_HEADER, "invalid-id"))
                .andExpect(status().isBadRequest());
    }
}
