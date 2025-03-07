CREATE TABLE Views (
    id CHAR(36) NOT NULL,
    article_id CHAR(36) NOT NULL,
    reader_id CHAR(36) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT view_article_fk FOREIGN KEY (article_id) REFERENCES Article(id),
    CONSTRAINT view_reader_fk FOREIGN KEY (reader_id) REFERENCES UserAccount(id)
);