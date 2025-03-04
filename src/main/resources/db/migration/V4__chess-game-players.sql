CREATE TABLE GamePlayers (
    chess_game_id CHAR(36) NOT NULL,
    player_for_white_id CHAR(36) NOT NULL,
    player_for_black_id CHAR(36) NOT NULL,
    PRIMARY KEY (
        chess_game_id,
        player_for_white_id,
        player_for_black_id
    ),
    CONSTRAINT game_players_fk FOREIGN KEY (chess_game_id) REFERENCES ChessGame(id),
    CONSTRAINT player_for_white_fk FOREIGN KEY (player_for_white_id) REFERENCES UserAccount(id),
    CONSTRAINT player_for_black_fk FOREIGN KEY (player_for_black_id) REFERENCES UserAccount(id)
);
