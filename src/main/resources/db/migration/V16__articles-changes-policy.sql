CREATE FUNCTION enforce_article_published() RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT status FROM Article WHERE id = NEW.article_id) <> 'PUBLISHED' THEN
        RAISE EXCEPTION 'Operation not allowed: Article must be PUBLISHED';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_comment_status
    BEFORE INSERT OR UPDATE OR DELETE ON Comments
    FOR EACH ROW
    EXECUTE FUNCTION enforce_article_published();

CREATE TRIGGER enforce_view_status
    BEFORE INSERT OR UPDATE OR DELETE ON Views
    FOR EACH ROW
    EXECUTE FUNCTION enforce_article_published();

CREATE TRIGGER enforce_like_status
    BEFORE INSERT OR UPDATE OR DELETE ON Likes
    FOR EACH ROW
    EXECUTE FUNCTION enforce_article_published();

CREATE TRIGGER enforce_tag_status
    BEFORE INSERT OR UPDATE OR DELETE ON ArticleTags
    FOR EACH ROW
    EXECUTE FUNCTION enforce_article_published();

CREATE FUNCTION enforce_article_published_on_comment_likes() RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT a.status FROM Article a JOIN Comments c ON c.id = NEW.comment_id) <> 'PUBLISHED' THEN
        RAISE EXCEPTION 'Operation not allowed: Article must be PUBLISHED';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_comment_like_status
    BEFORE INSERT OR UPDATE OR DELETE ON CommentLikes
    FOR EACH ROW
    EXECUTE FUNCTION enforce_article_published_on_comment_likes();