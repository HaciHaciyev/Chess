Create Table UserAccount (
    id char(36) not null,
    username varchar(32) not null,
    email varchar(255) not null,
    password varchar(128) not null,
    user_role varchar not null,
    is_enable boolean not null,
    rating real not null,
    rating_deviation real not null,
    rating_volatility real not null,
    creation_date timestamp not null,
    last_updated_date timestamp not null,
    primary key (id)
);

Create Unique Index
    user_email_index ON UserAccount (email);

Create Unique Index
    user_name_index ON UserAccount (username);