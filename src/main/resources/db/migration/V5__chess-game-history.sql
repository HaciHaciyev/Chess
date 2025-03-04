CREATE TABLE ChessGameHistory (
    id CHAR(36) NOT NULL,
    chess_game_id CHAR(36) NOT NULL,
    pgn_chess_representation VARCHAR NOT NULL,
    fen_representations_of_board TEXT [] NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT board_game_fk FOREIGN KEY (chess_game_id) REFERENCES ChessGame(id)
);
