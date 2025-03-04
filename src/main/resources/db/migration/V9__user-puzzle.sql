CREATE TABLE UserPuzzles (
    puzzle_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    is_solved boolean NOT NULL,
    PRIMARY KEY (
        puzzle_id,
        user_id
    ),
    CONSTRAINT puzzle_user_fk FOREIGN KEY (puzzle_id) REFERENCES Puzzle(id),
    CONSTRAINT user_puzzle_fk FOREIGN KEY (user_id) REFERENCES UserAccount(id)
);