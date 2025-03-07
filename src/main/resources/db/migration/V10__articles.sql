CREATE TABLE Articles (
    id CHAR(36) NOT NULL,
    author_id CHAR(36) NOT NULL,
    header TEXT NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(9) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);