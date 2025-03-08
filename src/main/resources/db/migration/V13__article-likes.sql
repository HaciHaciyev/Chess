CREATE TABLE Likes (
    article_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    PRIMARY KEY (article_id, user_id),
    CONSTRAINT like_article_fk FOREIGN KEY (article_id) REFERENCES Articles(id),
    CONSTRAINT like_user_fk FOREIGN KEY (user_id) REFERENCES UserAccount(id)
);