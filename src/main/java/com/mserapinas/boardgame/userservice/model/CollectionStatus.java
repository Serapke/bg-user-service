package com.mserapinas.boardgame.userservice.model;

public enum CollectionStatus {
    OWNED,          // The user owns this game
    WANT_TO_OWN,    // The user wants to acquire this game (wishlist)
    WANT_TO_PLAY    // The user wants to play this game (play backlog)
}
