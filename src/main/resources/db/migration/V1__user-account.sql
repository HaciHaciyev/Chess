Create Table UserAccount (
    id char(36) not null,
    username varchar(32) not null,
    email varchar(255) not null,
    password varchar(128) not null,
    rating smallint not null,
    isEnable boolean not null default true,
    creation_date timestamp not null,
    last_updated_date timestamp not null,
    primary key (id)
);

Create Unique Index
    user_email_index ON UserAccount (email);

Create Unique Index
    user_name_index ON UserAccount (username);