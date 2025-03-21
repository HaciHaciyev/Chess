CREATE TABLE CommentLikes (
    comment_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    CONSTRAINT like_comment_fk FOREIGN KEY (comment_id) REFERENCES Comments(id),
    CONSTRAINT comment_like_user_fk FOREIGN KEY (user_id) REFERENCES UserAccount(id)
);