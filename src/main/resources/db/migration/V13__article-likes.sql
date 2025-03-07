CREATE TABLE Like (
    article_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    PRIMARY KEY (article_id, user_id)
);