CREATE TABLE IF NOT EXISTS idin_records
(
    id int unsigned NOT NULL auto_increment PRIMARY KEY,
    bin varchar(256) NOT NULL,
    email varchar(256),
    time long NOT NULL
);
