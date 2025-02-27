Create Table ChessGame (
    id UUID not null,
    player_for_white_rating smallint not null,
    player_for_black_rating smallint not null,
    time_controlling_type varchar(7) not null,
    creation_date timestamp not null,
    last_updated_date timestamp not null,
    is_game_over boolean not null,
    primary key (id)
);