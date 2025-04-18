CREATE TABLE UserToken (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    token CHAR(36) NOT NULL,
    is_confirmed boolean NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT user_token_fk FOREIGN KEY (user_id) REFERENCES UserAccount(id)
);

CREATE FUNCTION delete_unconfirmed_user() RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM UserAccount
    WHERE id IN (
        SELECT user_id
        FROM UserToken
        WHERE is_confirmed = false
          AND creation_date <= NOW() - INTERVAL '6 minutes'
    );

    DELETE FROM UserToken
    WHERE is_confirmed = false
      AND creation_date <= NOW() - INTERVAL '6 minutes';
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION delete_confirmed_token() RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM UserToken
    WHERE is_confirmed = true;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_delete_unconfirmed_user
AFTER INSERT ON UserToken
FOR EACH STATEMENT
EXECUTE FUNCTION delete_unconfirmed_user();

CREATE TRIGGER trigger_delete_confirmed_token
AFTER INSERT OR UPDATE ON UserToken
FOR EACH STATEMENT
EXECUTE FUNCTION delete_confirmed_token();