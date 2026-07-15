package com.mserapinas.boardgame.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-off, idempotent backfill for the CollectionStatus enum rename
 * (OWN -> OWNED, WANT -> WANT_TO_OWN). Existing rows persisted the old
 * enum names as strings; without this they would fail to deserialize.
 * Safe to leave in place: it is a no-op once no legacy values remain.
 */
@Component
public class CollectionStatusBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectionStatusBackfill.class);

    private final JdbcTemplate jdbcTemplate;

    public CollectionStatusBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        int owned = jdbcTemplate.update(
            "UPDATE user_board_games SET status = 'OWNED' WHERE status = 'OWN'");
        int wantToOwn = jdbcTemplate.update(
            "UPDATE user_board_games SET status = 'WANT_TO_OWN' WHERE status = 'WANT'");

        if (owned > 0 || wantToOwn > 0) {
            log.info("CollectionStatus backfill: {} OWN->OWNED, {} WANT->WANT_TO_OWN", owned, wantToOwn);
        }
    }
}
