CREATE TABLE Puzzle (
    id UUID not null,
    rating real not null,
    rating_deviation real not null,
    rating_volatility real not null,
    startPositionFEN varchar not null,
    pgn varchar not null,
    primary key (id)
);