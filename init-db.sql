CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add collection_visibility column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='collection_visibility'
    ) THEN
        ALTER TABLE users ADD COLUMN collection_visibility VARCHAR(50);
        UPDATE users SET collection_visibility = 'FRIENDS' WHERE collection_visibility IS NULL;
        ALTER TABLE users ALTER COLUMN collection_visibility SET NOT NULL;
        ALTER TABLE users ALTER COLUMN collection_visibility SET DEFAULT 'FRIENDS';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS labels (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_labels_user_name UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS user_board_games (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    notes TEXT,
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_ubg_user_game UNIQUE (user_id, game_id)
);

-- Add status column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='user_board_games' AND column_name='status'
    ) THEN
        ALTER TABLE user_board_games ADD COLUMN status VARCHAR(50);
        UPDATE user_board_games SET status = 'OWN' WHERE status IS NULL;
        ALTER TABLE user_board_games ALTER COLUMN status SET NOT NULL;
        ALTER TABLE user_board_games ALTER COLUMN status SET DEFAULT 'OWN';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_board_game_labels (
    user_board_game_id INTEGER NOT NULL,
    label_id INTEGER NOT NULL,
    PRIMARY KEY(user_board_game_id, label_id),
    CONSTRAINT fk_user_board_game
        FOREIGN KEY(user_board_game_id)
        REFERENCES user_board_games(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_label
        FOREIGN KEY(label_id)
        REFERENCES labels(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ubgl_label_id ON user_board_game_labels(label_id);

CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 10),
    review_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_review
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_reviews_user_game UNIQUE (user_id, game_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_game_id ON reviews(game_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);

CREATE TABLE IF NOT EXISTS friendships (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_friendship_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_friendship_friend
        FOREIGN KEY(friend_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_not_self CHECK (user_id != friend_id),
    CONSTRAINT uq_friendships_user_friend UNIQUE (user_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_friend_id ON friendships(friend_id);
CREATE INDEX IF NOT EXISTS idx_friendships_user_id ON friendships(user_id);

CREATE TABLE IF NOT EXISTS game_plays (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    played_at DATE NOT NULL,
    times_played INTEGER NOT NULL DEFAULT 1 CHECK (times_played >= 1),
    duration_minutes INTEGER CHECK (duration_minutes > 0),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_play_user
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_game_plays_user_game ON game_plays(user_id, game_id);
CREATE INDEX IF NOT EXISTS idx_game_plays_user_played_at ON game_plays(user_id, played_at DESC);

CREATE TABLE IF NOT EXISTS game_play_players (
    game_play_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    PRIMARY KEY(game_play_id, player_id),
    CONSTRAINT fk_gpp_game_play
        FOREIGN KEY(game_play_id) REFERENCES game_plays(id) ON DELETE CASCADE,
    CONSTRAINT fk_gpp_player
        FOREIGN KEY(player_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS game_play_winners (
    game_play_id INTEGER NOT NULL,
    game_index INTEGER NOT NULL,
    winner_player_id INTEGER,
    PRIMARY KEY(game_play_id, game_index),
    CONSTRAINT fk_gpw_game_play
        FOREIGN KEY(game_play_id) REFERENCES game_plays(id) ON DELETE CASCADE,
    CONSTRAINT fk_gpw_winner
        FOREIGN KEY(winner_player_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Mock seed data
-- Note: Using BCrypt hash for password "Password123!"
INSERT INTO users (email, name, password)
VALUES
    ('kipras@example.com', 'Kipras', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('tautvydas@example.com', 'Tautvydas', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('justas@example.com', 'Justas', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('ignas@example.com', 'Ignas', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('paulius@example.com', 'Paulius', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
    ('mantas@example.com', 'Mantas', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
    ('darius@example.com', 'Darius', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
    ('mikhail@example.com', 'Mikhail', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
    ('viktorija@example.com', 'Viktorija', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
    ('ruta@example.com', 'Rūta', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
ON CONFLICT (email) DO NOTHING;

-- User board games
-- game IDs reference board_games in game-discovery-service:
--   1=CATAN, 48=Ticket to Ride Legacy: Legends of the West, 85=7 Wonders, 9=Codenames
INSERT INTO user_board_games (user_id, game_id, notes, status)
SELECT u.id, v.game_id, v.notes, v.status
FROM (
    VALUES
        ('kipras@example.com', 1, 'Owned', 'OWN'),
        ('kipras@example.com', 48, 'Legacy S1 complete', 'OWN'),
        ('tautvydas@example.com', 85, 'Wishlist', 'WANT'),
        ('tautvydas@example.com', 9, 'Owned', 'OWN')
) AS v(email, game_id, notes, status)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id, game_id) DO NOTHING;

-- Reviews (users can review games they don't own)
-- game IDs reference board_games in game-discovery-service:
--   1=CATAN, 48=Ticket to Ride Legacy: Legends of the West, 11=Pandemic, 85=7 Wonders
INSERT INTO reviews (user_id, game_id, rating, review_text)
SELECT u.id, v.game_id, v.rating, v.review_text
FROM (
    VALUES
        ('kipras@example.com', 1, 5, 'Amazing game! Perfect for game nights with friends.'),
        ('kipras@example.com', 48, 4, 'Great legacy experience, but takes commitment to finish.'),
        ('kipras@example.com', 11, 5, 'Best cooperative game I have ever played!'),
        ('tautvydas@example.com', 85, 5, 'Cannot wait to get this game. Watched many reviews and it looks fantastic!'),
        ('tautvydas@example.com', 1, 4, 'Solid game, though a bit too complex for casual players.')
) AS v(email, game_id, rating, review_text)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id, game_id) DO NOTHING;

-- Friendships
-- Kipras ↔ Tautvydas (accepted friendship - bidirectional)
INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'kipras@example.com' AND u2.email = 'tautvydas@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'tautvydas@example.com' AND u2.email = 'kipras@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

-- Kipras ↔ Ignas (accepted friendship - bidirectional)
INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'kipras@example.com' AND u2.email = 'ignas@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'ignas@example.com' AND u2.email = 'kipras@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

-- Ignas ↔ Paulius (accepted friendship - bidirectional)
INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'ignas@example.com' AND u2.email = 'paulius@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'paulius@example.com' AND u2.email = 'ignas@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;

-- Justas → Kipras (pending request - unidirectional)
INSERT INTO friendships (user_id, friend_id)
SELECT u1.id, u2.id
FROM users u1, users u2
WHERE u1.email = 'justas@example.com' AND u2.email = 'kipras@example.com'
ON CONFLICT (user_id, friend_id) DO NOTHING;