CREATE TABLE Puzzle (
    id CHAR(36) NOT NULL,
    rating REAL NOT NULL,
    rating_deviation REAL NOT NULL,
    rating_volatility REAL NOT NULL,
    startPositionFEN VARCHAR NOT NULL,
    pgn VARCHAR NOT NULL,
    startPositionIndex SMALLINT NOT NULL,
    PRIMARY KEY (id)
);