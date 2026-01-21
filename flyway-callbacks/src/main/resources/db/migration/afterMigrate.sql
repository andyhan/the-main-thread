INSERT INTO person (name, email)
VALUES
  ('Test User', 'test@example.com'),
  ('Admin User', 'admin@example.com')
ON CONFLICT (email) DO NOTHING;