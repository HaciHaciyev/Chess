CREATE TABLE Comments (
    id CHAR(36) NOT NULL,
    article_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    text VARCHAR(56) NOT NULL,
    comment_type VARCHAR(6) NOT NULL CHECK ( comment_type IN ('PARENT', 'CHILD')),
    parent_comment_id CHAR(36) NULL,
    respond_to_comment CHAR(36) NULL,
    creation_date TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT comment_article_fk FOREIGN KEY (article_id) REFERENCES Article(id),
    CONSTRAINT comment_user_fk FOREIGN KEY (user_id) REFERENCES UserAccount(id),
    CONSTRAINT parent_comment_id_fk FOREIGN KEY (parent_comment_id) REFERENCES Comments(id),
    CONSTRAINT respond_comment_id_fk FOREIGN KEY (respond_to_comment) REFERENCES Comments(id)
);

CREATE FUNCTION update_comment_date() RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = LOCALTIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER comment_update_tr
    BEFORE INSERT OR UPDATE ON Article
    FOR EACH ROW
    EXECUTE FUNCTION update_comment_date();