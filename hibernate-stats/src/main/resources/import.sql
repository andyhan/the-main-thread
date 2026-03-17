-- src/main/resources/import.sql
INSERT INTO author(id, name) VALUES (1, 'Ursula K. Le Guin');
INSERT INTO author(id, name) VALUES (51, 'Octavia Butler');

INSERT INTO book(id, title, author_id) VALUES (1,  'The Left Hand of Darkness', 1);
INSERT INTO book(id, title, author_id) VALUES (2,  'The Dispossessed',          1);
INSERT INTO book(id, title, author_id) VALUES (51, 'Kindred',                   51);
INSERT INTO book(id, title, author_id) VALUES (52, 'Parable of the Sower',      51);

ALTER SEQUENCE author_seq RESTART WITH 101;
ALTER SEQUENCE book_seq RESTART WITH 101;