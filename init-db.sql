CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
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

-- Mock seed data
-- Note: Using BCrypt hash for password "Password123!" 
INSERT INTO users (email, name, password)
VALUES
    ('alice@example.com', 'Alice', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('bob@example.com', 'Bob', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
ON CONFLICT (email) DO NOTHING;

-- User board games
INSERT INTO user_board_games (user_id, game_id, notes)
SELECT u.id, v.game_id, v.notes
FROM (
    VALUES
        ('alice@example.com', 1001, 'Owned'),
        ('alice@example.com', 1002, 'Legacy S1 complete'),
        ('bob@example.com', 2001, 'Wishlist'),
        ('bob@example.com', 2002, 'Owned')
) AS v(email, game_id, notes)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id, game_id) DO NOTHING;

-- Reviews (users can review games they don't own)
INSERT INTO reviews (user_id, game_id, rating, review_text)
SELECT u.id, v.game_id, v.rating, v.review_text
FROM (
    VALUES
        ('alice@example.com', 1001, 5, 'Amazing game! Perfect for game nights with friends.'),
        ('alice@example.com', 1002, 4, 'Great legacy experience, but takes commitment to finish.'),
        ('alice@example.com', 3001, 5, 'Best cooperative game I have ever played!'),
        ('bob@example.com', 2001, 5, 'Cannot wait to get this game. Watched many reviews and it looks fantastic!'),
        ('bob@example.com', 1001, 4, 'Solid game, though a bit too complex for casual players.')
) AS v(email, game_id, rating, review_text)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id, game_id) DO NOTHING;