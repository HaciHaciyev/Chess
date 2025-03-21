ALTER TABLE Comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE Views ENABLE ROW LEVEL SECURITY;
ALTER TABLE Likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE CommentLikes ENABLE ROW LEVEL SECURITY;

CREATE POLICY comments_select_policy ON Comments
    FOR SELECT USING (EXISTS (
        SELECT 1 FROM Article WHERE Article.id = Comments.article_id AND Article.status = 'PUBLISHED'
    ));

CREATE POLICY views_select_policy ON Views
    FOR SELECT USING (EXISTS (
        SELECT 1 FROM Article WHERE Article.id = Views.article_id AND Article.status = 'PUBLISHED'
    ));

CREATE POLICY likes_select_policy ON Likes
    FOR SELECT USING (EXISTS (
        SELECT 1 FROM Article WHERE Article.id = Likes.article_id AND Article.status = 'PUBLISHED'
    ));

CREATE POLICY comment_likes_select_policy ON CommentLikes
    FOR SELECT USING (EXISTS (
        SELECT 1 FROM Comments
        JOIN Article ON Comments.article_id = Article.id
        WHERE Comments.id = CommentLikes.comment_id AND Article.status = 'PUBLISHED'
    ));

ALTER TABLE Comments FORCE ROW LEVEL SECURITY;
ALTER TABLE Views FORCE ROW LEVEL SECURITY;
ALTER TABLE Likes FORCE ROW LEVEL SECURITY;
ALTER TABLE CommentLikes FORCE ROW LEVEL SECURITY;