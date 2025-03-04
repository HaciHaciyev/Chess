CREATE TABLE IF NOT EXISTS RefreshToken (
    user_id CHAR(36) NOT NULL,
    token text NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_account
        FOREIGN KEY (user_id) REFERENCES UserAccount(id)
        ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION prevent_user_id_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id <> OLD.user_id THEN
        RAISE EXCEPTION 'user_id cannot be modified.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_update_user_id
BEFORE UPDATE ON RefreshToken
FOR EACH ROW
EXECUTE FUNCTION prevent_user_id_update();

CREATE INDEX idx_refresh_token ON RefreshToken(token);