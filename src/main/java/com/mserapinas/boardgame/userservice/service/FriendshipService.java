package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.response.FriendListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendRequestDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendRequestListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendSuggestionDto;
import com.mserapinas.boardgame.userservice.exception.*;
import com.mserapinas.boardgame.userservice.model.Friendship;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.FriendRequestRepository;
import com.mserapinas.boardgame.userservice.repository.FriendshipRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             FriendRequestRepository friendRequestRepository,
                             UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
    }

    /**
     * Send a friend request
     * If the other user already sent a request, this auto-accepts the friendship
     */
    @Transactional
    public void sendFriendRequest(Long userId, Long friendId) {
        // Validation: no self-friending
        if (userId.equals(friendId)) {
            throw new SelfFriendshipException();
        }

        // Check if both users exist
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new UserNotFoundException(friendId));

        // Check if already friends
        if (friendshipRepository.areFriends(userId, friendId)) {
            throw new AlreadyFriendsException(userId, friendId);
        }

        // Check if request already sent
        if (friendRequestRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new FriendRequestAlreadySentException(userId, friendId);
        }

        // Create the friendship row
        // If friendId→userId already exists, this completes the bidirectional friendship
        // Otherwise, this creates a pending request
        Friendship request = new Friendship(user, friend);
        friendshipRepository.save(request);
    }

    /**
     * Accept an incoming friend request
     */
    @Transactional
    public void acceptFriendRequest(Long userId, Long requesterId) {
        // Verify incoming request exists
        if (!friendRequestRepository.hasPendingRequest(requesterId, userId)) {
            throw new FriendRequestNotFoundException(requesterId, userId);
        }

        // Check if not already friends
        if (friendshipRepository.areFriends(userId, requesterId)) {
            throw new AlreadyFriendsException(userId, requesterId);
        }

        // Get users
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new UserNotFoundException(requesterId));

        // Create reverse row to complete bidirectional friendship
        Friendship friendship = new Friendship(user, requester);
        friendshipRepository.save(friendship);
    }

    /**
     * Decline an incoming friend request
     */
    @Transactional
    public void declineFriendRequest(Long userId, Long requesterId) {
        // Verify incoming request exists
        if (!friendRequestRepository.hasPendingRequest(requesterId, userId)) {
            throw new FriendRequestNotFoundException(requesterId, userId);
        }

        // Delete the request row
        friendRequestRepository.deleteByUserIdAndFriendId(requesterId, userId);
    }

    /**
     * Cancel an outgoing friend request
     */
    @Transactional
    public void cancelFriendRequest(Long userId, Long friendId) {
        // Verify outgoing request exists
        if (!friendRequestRepository.hasPendingRequest(userId, friendId)) {
            throw new FriendRequestNotFoundException(userId, friendId);
        }

        // Delete the request row
        friendRequestRepository.deleteByUserIdAndFriendId(userId, friendId);
    }

    /**
     * Remove a friend (deletes both rows)
     */
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        // Verify bidirectional friendship exists
        if (!friendshipRepository.areFriends(userId, friendId)) {
            throw new FriendRequestNotFoundException(userId, friendId);
        }

        // Delete both rows (repository handles bidirectional deletion)
        friendshipRepository.deleteFriendship(userId, friendId);
    }

    /**
     * Get all friends of a user
     */
    public FriendListDto getFriends(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Friendship> friendships = friendshipRepository.getFriends(userId);
        return FriendListDto.from(friendships);
    }

    /**
     * Get incoming friend requests
     */
    public FriendRequestListDto getIncomingRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Friendship> requests = friendRequestRepository.getIncomingRequests(userId);
        List<FriendRequestDto> requestDtos = requests.stream()
            .map(FriendRequestDto::fromIncoming)
            .toList();

        return new FriendRequestListDto(requestDtos, (long) requestDtos.size());
    }

    /**
     * Get outgoing friend requests
     */
    public FriendRequestListDto getOutgoingRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Friendship> requests = friendRequestRepository.getOutgoingRequests(userId);
        List<FriendRequestDto> requestDtos = requests.stream()
            .map(FriendRequestDto::fromOutgoing)
            .toList();

        return new FriendRequestListDto(requestDtos, (long) requestDtos.size());
    }

    /**
     * Get friend suggestions (friends of friends who are NOT direct friends)
     */
    public FriendSuggestionDto getFriendSuggestions(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<User> suggestions = friendshipRepository.getFriendsOfFriends(userId);
        return FriendSuggestionDto.from(suggestions);
    }
}