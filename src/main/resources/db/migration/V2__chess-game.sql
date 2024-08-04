Create Table ChessGame (
    id char(36) not null,
    chess_board_id char(36) not null,
    player_for_white_rating smallint not null,
    player_for_black_rating smallint not null,
    time_controlling_type varchar(7) not null,
    creation_date timestamp not null,
    last_updated_date timestamp not null,
    primary key (id)
)

Create Table GamePlayers (
    chess_game_id char(36) not null,
    player_for_white_id char(36) not null,
    player_for_black_id char(36) not null,
    primary key (chess_game_id, player_for_white_id, player_for_black_id),
    constraint game_players_fk foreign key (chess_game_id) references ChessGame (id),
    constraint player_for_white_fk foreign key (player_for_white_id) references UserAccount (id),
    constraint player_for_black_fk foreign key (player_for_black_id) references UserAccount (id)
)