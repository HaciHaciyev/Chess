CREATE TABLE UserPartnership (
    user_id UUID not null,
    partner_id UUID not null,
    created_at timestamp not null,
    primary key (user_id, partner_id),
    foreign key (user_id) references UserAccount(id),
    foreign key (partner_id) references UserAccount(id)
);