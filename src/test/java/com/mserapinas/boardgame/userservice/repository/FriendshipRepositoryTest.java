package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.Friendship;
import com.mserapinas.boardgame.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendshipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "User One", "password");
        user2 = new User("user2@example.com", "User Two", "password");
        user3 = new User("user3@example.com", "User Three", "password");
        user4 = new User("user4@example.com", "User Four", "password");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.persist(user4);
        entityManager.flush();
    }

    // ========== Get Friends Tests ==========

    @Test
    @DisplayName("Should return friends when bidirectional friendship exists")
    void shouldReturnFriendsWhenBidirectionalFriendshipExists() {
        Friendship friendship1 = new Friendship(user1, user2);
        Friendship friendship2 = new Friendship(user2, user1);
        entityManager.persist(friendship1);
        entityManager.persist(friendship2);
        entityManager.flush();

        List<Friendship> friends = friendshipRepository.getFriends(user1.getId());

        assertThat(friends).hasSize(1);
        assertThat(friends.getFirst().getFriend().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should not return pending requests in friends list")
    void shouldNotReturnPendingRequestsInFriendsList() {
        Friendship pendingRequest = new Friendship(user1, user2);
        entityManager.persist(pendingRequest);
        entityManager.flush();

        List<Friendship> friends = friendshipRepository.getFriends(user1.getId());

        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple friends")
    void shouldReturnMultipleFriends() {
        Friendship friendship1to2 = new Friendship(user1, user2);
        Friendship friendship2to1 = new Friendship(user2, user1);
        Friendship friendship1to3 = new Friendship(user1, user3);
        Friendship friendship3to1 = new Friendship(user3, user1);

        entityManager.persist(friendship1to2);
        entityManager.persist(friendship2to1);
        entityManager.persist(friendship1to3);
        entityManager.persist(friendship3to1);
        entityManager.flush();

        List<Friendship> friends = friendshipRepository.getFriends(user1.getId());

        assertThat(friends).hasSize(2);
        assertThat(friends).extracting(f -> f.getFriend().getId())
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("Should return empty list when user has no friends")
    void shouldReturnEmptyListWhenUserHasNoFriends() {
        List<Friendship> friends = friendshipRepository.getFriends(user1.getId());

        assertThat(friends).isEmpty();
    }

    // ========== Get Friends of Friends Tests ==========

    @Test
    @DisplayName("Should return friends of friends excluding direct friends")
    void shouldReturnFriendsOfFriendsExcludingDirectFriends() {
        // user1 <-> user2 (friends)
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));

        // user2 <-> user3 (friends)
        entityManager.persist(new Friendship(user2, user3));
        entityManager.persist(new Friendship(user3, user2));

        // user2 <-> user4 (friends)
        entityManager.persist(new Friendship(user2, user4));
        entityManager.persist(new Friendship(user4, user2));
        entityManager.flush();

        List<User> suggestions = friendshipRepository.getFriendsOfFriends(user1.getId());

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions).extracting(User::getId)
                .containsExactlyInAnyOrder(user3.getId(), user4.getId());
    }

    @Test
    @DisplayName("Should not suggest users who are already direct friends")
    void shouldNotSuggestUsersWhoAreAlreadyDirectFriends() {
        // user1 <-> user2 (friends)
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));

        // user1 <-> user3 (already friends)
        entityManager.persist(new Friendship(user1, user3));
        entityManager.persist(new Friendship(user3, user1));

        // user2 <-> user3 (friends)
        entityManager.persist(new Friendship(user2, user3));
        entityManager.persist(new Friendship(user3, user2));
        entityManager.flush();

        List<User> suggestions = friendshipRepository.getFriendsOfFriends(user1.getId());

        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Should not suggest self")
    void shouldNotSuggestSelf() {
        // user1 <-> user2 (friends)
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));

        // user2 <-> user1 (already covered above, but checking circular case)
        entityManager.flush();

        List<User> suggestions = friendshipRepository.getFriendsOfFriends(user1.getId());

        assertThat(suggestions).doesNotContain(user1);
    }

    @Test
    @DisplayName("Should return empty list when no friends of friends exist")
    void shouldReturnEmptyListWhenNoFriendsOfFriendsExist() {
        List<User> suggestions = friendshipRepository.getFriendsOfFriends(user1.getId());

        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Should not suggest users from pending requests")
    void shouldNotSuggestUsersFromPendingRequests() {
        // user1 <-> user2 (friends)
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));

        // user2 -> user3 (pending request, not accepted)
        entityManager.persist(new Friendship(user2, user3));
        entityManager.flush();

        List<User> suggestions = friendshipRepository.getFriendsOfFriends(user1.getId());

        assertThat(suggestions).isEmpty();
    }

    // ========== Are Friends Tests ==========

    @Test
    @DisplayName("Should return true when users are friends")
    void shouldReturnTrueWhenUsersAreFriends() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        boolean areFriends = friendshipRepository.areFriends(user1.getId(), user2.getId());

        assertThat(areFriends).isTrue();
    }

    @Test
    @DisplayName("Should return false when only one direction exists")
    void shouldReturnFalseWhenOnlyOneDirectionExists() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.flush();

        boolean areFriends = friendshipRepository.areFriends(user1.getId(), user2.getId());

        assertThat(areFriends).isFalse();
    }

    @Test
    @DisplayName("Should return false when no friendship exists")
    void shouldReturnFalseWhenNoFriendshipExists() {
        boolean areFriends = friendshipRepository.areFriends(user1.getId(), user2.getId());

        assertThat(areFriends).isFalse();
    }

    @Test
    @DisplayName("Should work symmetrically for areFriends check")
    void shouldWorkSymmetricallyForAreFriendsCheck() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        boolean areFriends1to2 = friendshipRepository.areFriends(user1.getId(), user2.getId());
        boolean areFriends2to1 = friendshipRepository.areFriends(user2.getId(), user1.getId());

        assertThat(areFriends1to2).isTrue();
        assertThat(areFriends2to1).isTrue();
    }

    // ========== Delete Friendship Tests ==========

    @Test
    @DisplayName("Should delete friendship in both directions")
    void shouldDeleteFriendshipInBothDirections() {
        Friendship friendship1 = new Friendship(user1, user2);
        Friendship friendship2 = new Friendship(user2, user1);
        entityManager.persist(friendship1);
        entityManager.persist(friendship2);
        entityManager.flush();

        friendshipRepository.deleteFriendship(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendshipRepository.areFriends(user1.getId(), user2.getId())).isFalse();
    }

    @Test
    @DisplayName("Should handle delete when friendship does not exist")
    void shouldHandleDeleteWhenFriendshipDoesNotExist() {
        friendshipRepository.deleteFriendship(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendshipRepository.areFriends(user1.getId(), user2.getId())).isFalse();
    }

    @Test
    @DisplayName("Should delete only specified friendship, not others")
    void shouldDeleteOnlySpecifiedFriendshipNotOthers() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.persist(new Friendship(user3, user1));
        entityManager.flush();

        friendshipRepository.deleteFriendship(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendshipRepository.areFriends(user1.getId(), user2.getId())).isFalse();
        assertThat(friendshipRepository.areFriends(user1.getId(), user3.getId())).isTrue();
    }

    // ========== Count Friends Tests ==========

    @Test
    @DisplayName("Should count accepted friends correctly")
    void shouldCountAcceptedFriendsCorrectly() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.persist(new Friendship(user3, user1));
        entityManager.flush();

        Long count = friendshipRepository.countFriends(user1.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should not count pending requests in friend count")
    void shouldNotCountPendingRequestsInFriendCount() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.flush();

        Long count = friendshipRepository.countFriends(user1.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should return zero when user has no friends")
    void shouldReturnZeroWhenUserHasNoFriends() {
        Long count = friendshipRepository.countFriends(user1.getId());

        assertThat(count).isZero();
    }
}
