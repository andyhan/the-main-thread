-- Hibernate creates test_user_SEQ from @SequenceGenerator; we only seed data and advance the sequence
INSERT INTO test_user(id, username, password, role) VALUES (1, 'alice', 'alicePassword', 'admin');
INSERT INTO test_user(id, username, password, role) VALUES (2, 'bob', 'bobPassword', 'user');

SELECT setval('test_user_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM test_user));
