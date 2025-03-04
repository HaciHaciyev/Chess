CREATE TABLE UserPartnership (
    user_id CHAR(36) NOT NULL,
    partner_id CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (
        user_id,
        partner_id
    ),
    FOREIGN KEY (user_id) REFERENCES UserAccount(id),
    FOREIGN KEY (partner_id) REFERENCES UserAccount(id)
);