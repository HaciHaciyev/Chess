Create Table GamePlayers (
    chess_game_id char(36) not null,
    player_for_white_id char(36) not null,
    player_for_black_id char(36) not null,
    primary key (chess_game_id, player_for_white_id, player_for_black_id),
    constraint game_players_fk foreign key (chess_game_id) references ChessGame (id),
    constraint player_for_white_fk foreign key (player_for_white_id) references UserAccount (id),
    constraint player_for_black_fk foreign key (player_for_black_id) references UserAccount (id)
);