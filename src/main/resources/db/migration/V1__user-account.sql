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
    bullet_rating REAL NOT NULL,
    bullet_rating_deviation REAL NOT NULL,
    bullet_rating_volatility REAL NOT NULL,
    blitz_rating REAL NOT NULL,
    blitz_rating_deviation REAL NOT NULL,
    blitz_rating_volatility REAL NOT NULL,
    rapid_rating REAL NOT NULL,
    rapid_rating_deviation REAL NOT NULL,
    rapid_rating_volatility REAL NOT NULL,
    puzzles_rating REAL NOT NULL,
    puzzles_rating_deviation REAL NOT NULL,
    puzzles_rating_volatility REAL NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    last_updated_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX user_email_index ON UserAccount (email);

CREATE UNIQUE INDEX user_name_index ON UserAccount (username);

CREATE FUNCTION update_user_date() RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_date = LOCALTIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_account_update_tr
    BEFORE INSERT OR UPDATE ON UserAccount
    FOR EACH ROW
    EXECUTE FUNCTION update_user_date();