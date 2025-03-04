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