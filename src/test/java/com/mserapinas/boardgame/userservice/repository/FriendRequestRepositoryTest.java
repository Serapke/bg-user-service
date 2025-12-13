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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "User One", "password");
        user2 = new User("user2@example.com", "User Two", "password");
        user3 = new User("user3@example.com", "User Three", "password");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();
    }

    // ========== Get Outgoing Requests Tests ==========

    @Test
    @DisplayName("Should return outgoing pending requests")
    void shouldReturnOutgoingPendingRequests() {
        Friendship request = new Friendship(user1, user2);
        entityManager.persist(request);
        entityManager.flush();

        List<Friendship> outgoing = friendRequestRepository.getOutgoingRequests(user1.getId());

        assertThat(outgoing).hasSize(1);
        assertThat(outgoing.getFirst().getFriend().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should not return accepted friendships in outgoing requests")
    void shouldNotReturnAcceptedFriendshipsInOutgoingRequests() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        List<Friendship> outgoing = friendRequestRepository.getOutgoingRequests(user1.getId());

        assertThat(outgoing).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple outgoing requests")
    void shouldReturnMultipleOutgoingRequests() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.flush();

        List<Friendship> outgoing = friendRequestRepository.getOutgoingRequests(user1.getId());

        assertThat(outgoing).hasSize(2);
        assertThat(outgoing).extracting(f -> f.getFriend().getId())
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("Should return empty list when no outgoing requests")
    void shouldReturnEmptyListWhenNoOutgoingRequests() {
        List<Friendship> outgoing = friendRequestRepository.getOutgoingRequests(user1.getId());

        assertThat(outgoing).isEmpty();
    }

    // ========== Get Incoming Requests Tests ==========

    @Test
    @DisplayName("Should return incoming pending requests")
    void shouldReturnIncomingPendingRequests() {
        Friendship request = new Friendship(user2, user1);
        entityManager.persist(request);
        entityManager.flush();

        List<Friendship> incoming = friendRequestRepository.getIncomingRequests(user1.getId());

        assertThat(incoming).hasSize(1);
        assertThat(incoming.getFirst().getUser().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should not return accepted friendships in incoming requests")
    void shouldNotReturnAcceptedFriendshipsInIncomingRequests() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        List<Friendship> incoming = friendRequestRepository.getIncomingRequests(user1.getId());

        assertThat(incoming).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple incoming requests")
    void shouldReturnMultipleIncomingRequests() {
        entityManager.persist(new Friendship(user2, user1));
        entityManager.persist(new Friendship(user3, user1));
        entityManager.flush();

        List<Friendship> incoming = friendRequestRepository.getIncomingRequests(user1.getId());

        assertThat(incoming).hasSize(2);
        assertThat(incoming).extracting(f -> f.getUser().getId())
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("Should return empty list when no incoming requests")
    void shouldReturnEmptyListWhenNoIncomingRequests() {
        List<Friendship> incoming = friendRequestRepository.getIncomingRequests(user1.getId());

        assertThat(incoming).isEmpty();
    }

    // ========== Has Pending Request Tests ==========

    @Test
    @DisplayName("Should return true when pending request exists")
    void shouldReturnTrueWhenPendingRequestExists() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.flush();

        boolean hasPending = friendRequestRepository.hasPendingRequest(user1.getId(), user2.getId());

        assertThat(hasPending).isTrue();
    }

    @Test
    @DisplayName("Should return false when no pending request exists")
    void shouldReturnFalseWhenNoPendingRequestExists() {
        boolean hasPending = friendRequestRepository.hasPendingRequest(user1.getId(), user2.getId());

        assertThat(hasPending).isFalse();
    }

    @Test
    @DisplayName("Should return false when friendship is accepted")
    void shouldReturnFalseWhenFriendshipIsAccepted() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        boolean hasPending = friendRequestRepository.hasPendingRequest(user1.getId(), user2.getId());

        assertThat(hasPending).isFalse();
    }

    @Test
    @DisplayName("Should be directional for pending requests")
    void shouldBeDirectionalForPendingRequests() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.flush();

        boolean user1ToUser2 = friendRequestRepository.hasPendingRequest(user1.getId(), user2.getId());
        boolean user2ToUser1 = friendRequestRepository.hasPendingRequest(user2.getId(), user1.getId());

        assertThat(user1ToUser2).isTrue();
        assertThat(user2ToUser1).isFalse();
    }

    // ========== Find Pending Request Tests ==========

    @Test
    @DisplayName("Should find pending request when exists")
    void shouldFindPendingRequestWhenExists() {
        Friendship request = new Friendship(user1, user2);
        entityManager.persist(request);
        entityManager.flush();

        Optional<Friendship> found = friendRequestRepository.findPendingRequest(user1.getId(), user2.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getFriend().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should return empty when no pending request exists")
    void shouldReturnEmptyWhenNoPendingRequestExists() {
        Optional<Friendship> found = friendRequestRepository.findPendingRequest(user1.getId(), user2.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when friendship is accepted")
    void shouldReturnEmptyWhenFriendshipIsAccepted() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        Optional<Friendship> found = friendRequestRepository.findPendingRequest(user1.getId(), user2.getId());

        assertThat(found).isEmpty();
    }

    // ========== Exists By User ID And Friend ID Tests ==========

    @Test
    @DisplayName("Should return true when any relationship exists")
    void shouldReturnTrueWhenAnyRelationshipExists() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.flush();

        boolean exists = friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return true for accepted friendship")
    void shouldReturnTrueForAcceptedFriendship() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        boolean exists = friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when no relationship exists")
    void shouldReturnFalseWhenNoRelationshipExists() {
        boolean exists = friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should be directional for exists check")
    void shouldBeDirectionalForExistsCheck() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.flush();

        boolean user1ToUser2 = friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId());
        boolean user2ToUser1 = friendRequestRepository.existsByUserIdAndFriendId(user2.getId(), user1.getId());

        assertThat(user1ToUser2).isTrue();
        assertThat(user2ToUser1).isFalse();
    }

    // ========== Delete By User ID And Friend ID Tests ==========

    @Test
    @DisplayName("Should delete specific request direction only")
    void shouldDeleteSpecificRequestDirectionOnly() {
        Friendship request1 = new Friendship(user1, user2);
        Friendship request2 = new Friendship(user2, user1);
        entityManager.persist(request1);
        entityManager.persist(request2);
        entityManager.flush();

        friendRequestRepository.deleteByUserIdAndFriendId(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId())).isFalse();
        assertThat(friendRequestRepository.existsByUserIdAndFriendId(user2.getId(), user1.getId())).isTrue();
    }

    @Test
    @DisplayName("Should handle delete when request does not exist")
    void shouldHandleDeleteWhenRequestDoesNotExist() {
        friendRequestRepository.deleteByUserIdAndFriendId(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId())).isFalse();
    }

    @Test
    @DisplayName("Should delete only specified request, not others")
    void shouldDeleteOnlySpecifiedRequestNotOthers() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.flush();

        friendRequestRepository.deleteByUserIdAndFriendId(user1.getId(), user2.getId());
        entityManager.flush();

        assertThat(friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user2.getId())).isFalse();
        assertThat(friendRequestRepository.existsByUserIdAndFriendId(user1.getId(), user3.getId())).isTrue();
    }

    // ========== Count Outgoing Requests Tests ==========

    @Test
    @DisplayName("Should count outgoing pending requests correctly")
    void shouldCountOutgoingPendingRequestsCorrectly() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user1, user3));
        entityManager.flush();

        Long count = friendRequestRepository.countOutgoingRequests(user1.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should not count accepted friendships in outgoing count")
    void shouldNotCountAcceptedFriendshipsInOutgoingCount() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        Long count = friendRequestRepository.countOutgoingRequests(user1.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should return zero when no outgoing requests")
    void shouldReturnZeroWhenNoOutgoingRequests() {
        Long count = friendRequestRepository.countOutgoingRequests(user1.getId());

        assertThat(count).isZero();
    }

    // ========== Count Incoming Requests Tests ==========

    @Test
    @DisplayName("Should count incoming pending requests correctly")
    void shouldCountIncomingPendingRequestsCorrectly() {
        entityManager.persist(new Friendship(user2, user1));
        entityManager.persist(new Friendship(user3, user1));
        entityManager.flush();

        Long count = friendRequestRepository.countIncomingRequests(user1.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should not count accepted friendships in incoming count")
    void shouldNotCountAcceptedFriendshipsInIncomingCount() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user2, user1));
        entityManager.flush();

        Long count = friendRequestRepository.countIncomingRequests(user1.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should return zero when no incoming requests")
    void shouldReturnZeroWhenNoIncomingRequests() {
        Long count = friendRequestRepository.countIncomingRequests(user1.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should differentiate between incoming and outgoing in counts")
    void shouldDifferentiateBetweenIncomingAndOutgoingInCounts() {
        entityManager.persist(new Friendship(user1, user2));
        entityManager.persist(new Friendship(user3, user1));
        entityManager.flush();

        Long outgoingCount = friendRequestRepository.countOutgoingRequests(user1.getId());
        Long incomingCount = friendRequestRepository.countIncomingRequests(user1.getId());

        assertThat(outgoingCount).isEqualTo(1L);
        assertThat(incomingCount).isEqualTo(1L);
    }
}
