CREATE TABLE Puzzle (
    id char(36) not null,
    rating real not null,
    rating_deviation real not null,
    rating_volatility real not null,
    startPositionFEN varchar not null,
    pgn varchar not null,
    startPositionIndex smallint not null,
    primary key (id)
);