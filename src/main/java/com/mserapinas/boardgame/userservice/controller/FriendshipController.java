package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.response.FriendListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendRequestListDto;
import com.mserapinas.boardgame.userservice.dto.response.FriendSuggestionDto;
import com.mserapinas.boardgame.userservice.service.FriendshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    /**
     * Send a friend request
     * POST /api/v1/friendships/requests/{friendUserId}
     */
    @PostMapping("/requests/{friendUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendFriendRequest(
            @CurrentUser Long userId,
            @PathVariable Long friendUserId) {
        friendshipService.sendFriendRequest(userId, friendUserId);
    }

    /**
     * Accept an incoming friend request
     * POST /api/v1/friendships/requests/{requesterUserId}/accept
     */
    @PostMapping("/requests/{requesterUserId}/accept")
    @ResponseStatus(HttpStatus.OK)
    public void acceptFriendRequest(
            @CurrentUser Long userId,
            @PathVariable Long requesterUserId) {
        friendshipService.acceptFriendRequest(userId, requesterUserId);
    }

    /**
     * Decline an incoming friend request
     * DELETE /api/v1/friendships/requests/{requesterUserId}/decline
     */
    @DeleteMapping("/requests/{requesterUserId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void declineFriendRequest(
            @CurrentUser Long userId,
            @PathVariable Long requesterUserId) {
        friendshipService.declineFriendRequest(userId, requesterUserId);
    }

    /**
     * Cancel an outgoing friend request
     * DELETE /api/v1/friendships/requests/{friendUserId}/cancel
     */
    @DeleteMapping("/requests/{friendUserId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelFriendRequest(
            @CurrentUser Long userId,
            @PathVariable Long friendUserId) {
        friendshipService.cancelFriendRequest(userId, friendUserId);
    }

    /**
     * Get incoming friend requests
     * GET /api/v1/friendships/requests/incoming
     */
    @GetMapping("/requests/incoming")
    public ResponseEntity<FriendRequestListDto> getIncomingRequests(@CurrentUser Long userId) {
        FriendRequestListDto requests = friendshipService.getIncomingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get outgoing friend requests
     * GET /api/v1/friendships/requests/outgoing
     */
    @GetMapping("/requests/outgoing")
    public ResponseEntity<FriendRequestListDto> getOutgoingRequests(@CurrentUser Long userId) {
        FriendRequestListDto requests = friendshipService.getOutgoingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get all friends
     * GET /api/v1/friendships
     */
    @GetMapping
    public ResponseEntity<FriendListDto> getFriends(@CurrentUser Long userId) {
        FriendListDto friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    /**
     * Remove a friend
     * DELETE /api/v1/friendships/{friendUserId}
     */
    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(
            @CurrentUser Long userId,
            @PathVariable Long friendUserId) {
        friendshipService.removeFriend(userId, friendUserId);
    }

    /**
     * Get friend suggestions (friends of friends)
     * GET /api/v1/friendships/suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<FriendSuggestionDto> getFriendSuggestions(@CurrentUser Long userId) {
        FriendSuggestionDto suggestions = friendshipService.getFriendSuggestions(userId);
        return ResponseEntity.ok(suggestions);
    }
}