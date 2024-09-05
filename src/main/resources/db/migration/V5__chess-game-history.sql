Create Table ChessGameHistory (
    id char(36) not null,
    chess_game_id char(36) not null,
    pgn_chess_representation varchar not null,
    fen_representations_of_board text[] not null,
    primary key (id),
    constraint board_game_fk foreign key (chess_game_id) references ChessGame (id)
);

Alter table ChessGame add constraint game_board_fk foreign key (chess_board_id) references ChessGameHistory (id);