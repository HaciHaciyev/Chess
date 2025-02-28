CREATE TABLE UserPartnership (
    user_id char(36) not null,
    partner_id char(36) not null,
    created_at timestamp not null,
    primary key (user_id, partner_id),
    foreign key (user_id) references UserAccount(id),
    foreign key (partner_id) references UserAccount(id)
);