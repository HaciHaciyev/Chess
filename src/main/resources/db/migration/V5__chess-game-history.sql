Create Table ChessGameHistory (
    id UUID not null,
    chess_game_id UUID not null,
    pgn_chess_representation varchar not null,
    fen_representations_of_board text[] not null,
    primary key (id),
    constraint board_game_fk foreign key (chess_game_id) references ChessGame (id)
);