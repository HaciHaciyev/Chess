CREATE TABLE Puzzle (
    id UUID not null,
    startPositionFEN varchar not null,
    pgn varchar not null,
    primary key (id)
);