ALTER TABLE UserAccount
    ADD COLUMN puzzles_rating real not null;
ALTER TABLE UserAccount
    ADD COLUMN puzzles_rating_deviation real not null;
ALTER TABLE UserAccount
    ADD COLUMN puzzles_rating_volatility real not null;