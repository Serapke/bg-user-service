package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.response.FriendListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendRequestListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendSuggestionDto;
import com.mserapinas.boardgame.userservice.exception.*;
import com.mserapinas.boardgame.userservice.model.Friendship;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.FriendRequestRepository;
import com.mserapinas.boardgame.userservice.repository.FriendshipRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendshipService friendshipService;

    private User user1;
    private User user2;
    private static final Long USER_1_ID = 1L;
    private static final Long USER_2_ID = 2L;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "User One", "password");
        user1.setId(USER_1_ID);

        user2 = new User("user2@example.com", "User Two", "password");
        user2.setId(USER_2_ID);
    }

    // ========== Send Friend Request Tests ==========

    @Test
    @DisplayName("Should send friend request successfully when users exist and not friends")
    void shouldSendFriendRequestSuccessfully() {
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);
        when(friendRequestRepository.existsByUserIdAndFriendId(USER_1_ID, USER_2_ID)).thenReturn(false);

        friendshipService.sendFriendRequest(USER_1_ID, USER_2_ID);

        verify(friendshipRepository).save(any(Friendship.class));
        verify(userRepository).findById(USER_1_ID);
        verify(userRepository).findById(USER_2_ID);
        verify(friendshipRepository).areFriends(USER_1_ID, USER_2_ID);
        verify(friendRequestRepository).existsByUserIdAndFriendId(USER_1_ID, USER_2_ID);
    }

    @Test
    @DisplayName("Should throw SelfFriendshipException when trying to friend yourself")
    void shouldThrowSelfFriendshipExceptionWhenFriendingYourself() {
        assertThatThrownBy(() -> friendshipService.sendFriendRequest(USER_1_ID, USER_1_ID))
                .isInstanceOf(SelfFriendshipException.class);

        verify(userRepository, never()).findById(any());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when sender does not exist")
    void shouldThrowUserNotFoundExceptionWhenSenderDoesNotExist() {
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(USER_1_ID);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when friend does not exist")
    void shouldThrowUserNotFoundExceptionWhenFriendDoesNotExist() {
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(USER_1_ID);
        verify(userRepository).findById(USER_2_ID);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AlreadyFriendsException when users are already friends")
    void shouldThrowAlreadyFriendsExceptionWhenAlreadyFriends() {
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(AlreadyFriendsException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FriendRequestAlreadySentException when request already sent")
    void shouldThrowFriendRequestAlreadySentExceptionWhenRequestAlreadySent() {
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);
        when(friendRequestRepository.existsByUserIdAndFriendId(USER_1_ID, USER_2_ID)).thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(FriendRequestAlreadySentException.class);

        verify(friendshipRepository, never()).save(any());
    }

    // ========== Accept Friend Request Tests ==========

    @Test
    @DisplayName("Should accept friend request successfully")
    void shouldAcceptFriendRequestSuccessfully() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(true);
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.of(user2));

        friendshipService.acceptFriendRequest(USER_1_ID, USER_2_ID);

        verify(friendshipRepository).save(any(Friendship.class));
        verify(friendRequestRepository).hasPendingRequest(USER_2_ID, USER_1_ID);
        verify(friendshipRepository).areFriends(USER_1_ID, USER_2_ID);
    }

    @Test
    @DisplayName("Should throw FriendRequestNotFoundException when accepting non-existent request")
    void shouldThrowFriendRequestNotFoundExceptionWhenAcceptingNonExistentRequest() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(FriendRequestNotFoundException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AlreadyFriendsException when accepting from already friend")
    void shouldThrowAlreadyFriendsExceptionWhenAcceptingFromAlreadyFriend() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(true);
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(true);

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(AlreadyFriendsException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when accepting user does not exist")
    void shouldThrowUserNotFoundExceptionWhenAcceptingUserDoesNotExist() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(true);
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when requester does not exist")
    void shouldThrowUserNotFoundExceptionWhenRequesterDoesNotExist() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(true);
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);
        when(userRepository.findById(USER_1_ID)).thenReturn(Optional.of(user1));
        when(userRepository.findById(USER_2_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendshipRepository, never()).save(any());
    }

    // ========== Decline Friend Request Tests ==========

    @Test
    @DisplayName("Should decline friend request successfully")
    void shouldDeclineFriendRequestSuccessfully() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(true);

        friendshipService.declineFriendRequest(USER_1_ID, USER_2_ID);

        verify(friendRequestRepository).deleteByUserIdAndFriendId(USER_2_ID, USER_1_ID);
        verify(friendRequestRepository).hasPendingRequest(USER_2_ID, USER_1_ID);
    }

    @Test
    @DisplayName("Should throw FriendRequestNotFoundException when declining non-existent request")
    void shouldThrowFriendRequestNotFoundExceptionWhenDecliningNonExistentRequest() {
        when(friendRequestRepository.hasPendingRequest(USER_2_ID, USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.declineFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(FriendRequestNotFoundException.class);

        verify(friendRequestRepository, never()).deleteByUserIdAndFriendId(any(), any());
    }

    // ========== Cancel Friend Request Tests ==========

    @Test
    @DisplayName("Should cancel friend request successfully")
    void shouldCancelFriendRequestSuccessfully() {
        when(friendRequestRepository.hasPendingRequest(USER_1_ID, USER_2_ID)).thenReturn(true);

        friendshipService.cancelFriendRequest(USER_1_ID, USER_2_ID);

        verify(friendRequestRepository).deleteByUserIdAndFriendId(USER_1_ID, USER_2_ID);
        verify(friendRequestRepository).hasPendingRequest(USER_1_ID, USER_2_ID);
    }

    @Test
    @DisplayName("Should throw FriendRequestNotFoundException when canceling non-existent request")
    void shouldThrowFriendRequestNotFoundExceptionWhenCancelingNonExistentRequest() {
        when(friendRequestRepository.hasPendingRequest(USER_1_ID, USER_2_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.cancelFriendRequest(USER_1_ID, USER_2_ID))
                .isInstanceOf(FriendRequestNotFoundException.class);

        verify(friendRequestRepository, never()).deleteByUserIdAndFriendId(any(), any());
    }

    // ========== Remove Friend Tests ==========

    @Test
    @DisplayName("Should remove friend successfully")
    void shouldRemoveFriendSuccessfully() {
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(true);

        friendshipService.removeFriend(USER_1_ID, USER_2_ID);

        verify(friendshipRepository).deleteFriendship(USER_1_ID, USER_2_ID);
        verify(friendshipRepository).areFriends(USER_1_ID, USER_2_ID);
    }

    @Test
    @DisplayName("Should throw FriendRequestNotFoundException when removing non-existent friend")
    void shouldThrowFriendRequestNotFoundExceptionWhenRemovingNonExistentFriend() {
        when(friendshipRepository.areFriends(USER_1_ID, USER_2_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.removeFriend(USER_1_ID, USER_2_ID))
                .isInstanceOf(FriendRequestNotFoundException.class);

        verify(friendshipRepository, never()).deleteFriendship(any(), any());
    }

    // ========== Get Friends Tests ==========

    @Test
    @DisplayName("Should get friends list successfully")
    void shouldGetFriendsListSuccessfully() {
        Friendship friendship = new Friendship(user1, user2);
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendshipRepository.getFriends(USER_1_ID)).thenReturn(List.of(friendship));

        FriendListDto result = friendshipService.getFriends(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.friends()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        verify(userRepository).existsById(USER_1_ID);
        verify(friendshipRepository).getFriends(USER_1_ID);
    }

    @Test
    @DisplayName("Should return empty list when user has no friends")
    void shouldReturnEmptyListWhenUserHasNoFriends() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendshipRepository.getFriends(USER_1_ID)).thenReturn(Collections.emptyList());

        FriendListDto result = friendshipService.getFriends(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.friends()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when getting friends for non-existent user")
    void shouldThrowUserNotFoundExceptionWhenGettingFriendsForNonExistentUser() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.getFriends(USER_1_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendshipRepository, never()).getFriends(any());
    }

    // ========== Get Incoming Requests Tests ==========

    @Test
    @DisplayName("Should get incoming friend requests successfully")
    void shouldGetIncomingFriendRequestsSuccessfully() {
        Friendship request = new Friendship(user2, user1);
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendRequestRepository.getIncomingRequests(USER_1_ID)).thenReturn(List.of(request));

        FriendRequestListDto result = friendshipService.getIncomingRequests(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.requests()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        verify(userRepository).existsById(USER_1_ID);
        verify(friendRequestRepository).getIncomingRequests(USER_1_ID);
    }

    @Test
    @DisplayName("Should return empty list when user has no incoming requests")
    void shouldReturnEmptyListWhenUserHasNoIncomingRequests() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendRequestRepository.getIncomingRequests(USER_1_ID)).thenReturn(Collections.emptyList());

        FriendRequestListDto result = friendshipService.getIncomingRequests(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.requests()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when getting incoming requests for non-existent user")
    void shouldThrowUserNotFoundExceptionWhenGettingIncomingRequestsForNonExistentUser() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.getIncomingRequests(USER_1_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendRequestRepository, never()).getIncomingRequests(any());
    }

    // ========== Get Outgoing Requests Tests ==========

    @Test
    @DisplayName("Should get outgoing friend requests successfully")
    void shouldGetOutgoingFriendRequestsSuccessfully() {
        Friendship request = new Friendship(user1, user2);
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendRequestRepository.getOutgoingRequests(USER_1_ID)).thenReturn(List.of(request));

        FriendRequestListDto result = friendshipService.getOutgoingRequests(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.requests()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        verify(userRepository).existsById(USER_1_ID);
        verify(friendRequestRepository).getOutgoingRequests(USER_1_ID);
    }

    @Test
    @DisplayName("Should return empty list when user has no outgoing requests")
    void shouldReturnEmptyListWhenUserHasNoOutgoingRequests() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendRequestRepository.getOutgoingRequests(USER_1_ID)).thenReturn(Collections.emptyList());

        FriendRequestListDto result = friendshipService.getOutgoingRequests(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.requests()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when getting outgoing requests for non-existent user")
    void shouldThrowUserNotFoundExceptionWhenGettingOutgoingRequestsForNonExistentUser() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.getOutgoingRequests(USER_1_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendRequestRepository, never()).getOutgoingRequests(any());
    }

    // ========== Get Friend Suggestions Tests ==========

    @Test
    @DisplayName("Should get friend suggestions successfully")
    void shouldGetFriendSuggestionsSuccessfully() {
        User user3 = new User("user3@example.com", "User Three", "password");
        user3.setId(3L);

        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendshipRepository.getFriendsOfFriends(USER_1_ID)).thenReturn(List.of(user3));

        FriendSuggestionDto result = friendshipService.getFriendSuggestions(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        verify(userRepository).existsById(USER_1_ID);
        verify(friendshipRepository).getFriendsOfFriends(USER_1_ID);
    }

    @Test
    @DisplayName("Should return empty list when user has no friend suggestions")
    void shouldReturnEmptyListWhenUserHasNoFriendSuggestions() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(true);
        when(friendshipRepository.getFriendsOfFriends(USER_1_ID)).thenReturn(Collections.emptyList());

        FriendSuggestionDto result = friendshipService.getFriendSuggestions(USER_1_ID);

        assertThat(result).isNotNull();
        assertThat(result.suggestions()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when getting suggestions for non-existent user")
    void shouldThrowUserNotFoundExceptionWhenGettingSuggestionsForNonExistentUser() {
        when(userRepository.existsById(USER_1_ID)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.getFriendSuggestions(USER_1_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendshipRepository, never()).getFriendsOfFriends(any());
    }
}
