CREATE TABLE Comments (
    id CHAR(36) NOT NULL,
    article_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    comment_type VARCHAR(6) NOT NULL,
    parent_comment_id CHAR(36) NULL,
    respond_to_comment CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT comment_article_fk FOREIGN KEY (article_id) REFERENCES Article(id),
    CONSTRAINT comment_user_fk FOREIGN KEY (user_id) REFERENCES UserAccount(user_id),
    CONSTRAINT parent_comment_id_fk FOREIGN KEY (parent_comment_id) REFERENCES Comments(id),
    CONSTRAINT respond_comment_id_fk FOREIGN KEY (respond_to_comment) REFERENCES Comments(id)
);