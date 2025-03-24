CREATE TABLE Article (
    id CHAR(36) NOT NULL,
    author_id CHAR(36) NOT NULL,
    header TEXT NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(9) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    creation_date TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    search_document tsvector,
    PRIMARY KEY (id)
);

CREATE INDEX article_search_idx ON Article USING GIN(search_document);

CREATE FUNCTION update_search_document() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_document := to_tsvector('english', NEW.header || ' ' || NEW.summary || ' ' || NEW.body);
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER article_search_update
    BEFORE INSERT OR UPDATE ON Article
        FOR EACH ROW
        EXECUTE FUNCTION update_search_document();