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

CREATE FUNCTION delete_confirmed_token() RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM UserToken
    WHERE is_confirmed = true
      AND user_id = NEW.user_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_delete_confirmed_token
AFTER INSERT OR UPDATE ON UserToken
FOR EACH STATEMENT
EXECUTE FUNCTION delete_confirmed_token();