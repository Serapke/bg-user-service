package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.User;

import java.util.List;

public record FriendSuggestionDto(
    List<UserSuggestionDto> suggestions,
    Long totalCount
) {
    public static FriendSuggestionDto from(List<User> users) {
        List<UserSuggestionDto> suggestions = users.stream()
            .map(UserSuggestionDto::from)
            .toList();
        return new FriendSuggestionDto(suggestions, (long) suggestions.size());
    }
}

record UserSuggestionDto(
    Long userId,
    String userName,
    String email
) {
    public static UserSuggestionDto from(User user) {
        return new UserSuggestionDto(
            user.getId(),
            user.getName(),
            user.getEmail()
        );
    }
}