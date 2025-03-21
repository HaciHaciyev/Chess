CREATE TABLE ChessGame (
    id CHAR(36) NOT NULL,
    player_for_white_rating SMALLINT NOT NULL,
    player_for_black_rating SMALLINT NOT NULL,
    time_controlling_type VARCHAR(7) NOT NULL CHECK ( time_controlling_type IN ('BULLET', 'BLITZ', 'RAPID', 'CLASSIC', 'DEFAULT')),
    creation_date TIMESTAMP NOT NULL,
    last_updated_date TIMESTAMP NOT NULL,
    is_game_over boolean NOT NULL,
    game_result_status VARCHAR(10) NOT NULL,
    PRIMARY KEY (id)
);