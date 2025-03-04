CREATE TABLE UserAccount (
    id CHAR(36) NOT NULL,
    firstname VARCHAR(48) NOT NULL,
    surname VARCHAR(56) NOT NULL,
    username VARCHAR(32) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(128) NOT NULL,
    user_role VARCHAR NOT NULL,
    is_enable boolean NOT NULL,
    rating REAL NOT NULL,
    rating_deviation REAL NOT NULL,
    rating_volatility REAL NOT NULL,
    puzzles_rating REAL NOT NULL,
    puzzles_rating_deviation REAL NOT NULL,
    puzzles_rating_volatility REAL NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    last_updated_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX user_email_index ON UserAccount (email);

CREATE UNIQUE INDEX user_name_index ON UserAccount (username);