CREATE TABLE UserPuzzles (
    puzzle_id char(36) not null,
    user_id char(36) not null,
    is_solved boolean not null,
    primary key (puzzle_id, user_id),
    constraint puzzle_user_fk foreign key (puzzle_id) references Puzzle (id),
    constraint user_puzzle_fk foreign key (user_id) references UserAccount (id)
);