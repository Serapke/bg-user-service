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

-- Mock seed data
-- Note: Using BCrypt hash for password "Password123!" 
INSERT INTO users (email, name, password)
VALUES
    ('alice@example.com', 'Alice', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG'),
    ('bob@example.com', 'Bob', '$2a$10$8K1p/a0dQ2TQxa21LDbODeAmhltJL0.pCGhrtwEzMo2vREQpgS8TG')
ON CONFLICT (email) DO NOTHING;

-- Labels per user
INSERT INTO labels (user_id, name)
SELECT u.id, v.label_name
FROM (
    VALUES
        ('alice@example.com', 'Strategy'),
        ('alice@example.com', 'Family'),
        ('alice@example.com', 'Co-op'),
        ('bob@example.com', 'Strategy'),
        ('bob@example.com', 'Family')
) AS v(email, label_name)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id, name) DO NOTHING;

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

-- Link labels to user board games
INSERT INTO user_board_game_labels (user_board_game_id, label_id)
SELECT ubg.id, l.id
FROM (
    VALUES
        ('alice@example.com', 1001, 'Strategy'),
        ('alice@example.com', 1001, 'Family'),
        ('alice@example.com', 1002, 'Co-op'),
        ('bob@example.com', 2001, 'Strategy'),
        ('bob@example.com', 2002, 'Family')
) AS v(email, game_id, label_name)
JOIN users u ON u.email = v.email
JOIN user_board_games ubg ON ubg.user_id = u.id AND ubg.game_id = v.game_id
JOIN labels l ON l.user_id = u.id AND l.name = v.label_name
ON CONFLICT DO NOTHING;