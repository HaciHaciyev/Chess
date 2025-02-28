Create table UserToken(
    id char(36) not null,
    user_id char(36) not null,
    token char(36) not null,
    is_confirmed boolean not null,
    creation_date timestamp not null,
    expiration_date timestamp not null,
    primary key (id),
    constraint user_token_fk foreign key (user_id) references UserAccount (id)
);