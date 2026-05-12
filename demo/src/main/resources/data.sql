-- =====================================================================
-- Seed Data for Testing
-- =====================================================================

-- Test users (password is "password123" hashed with SHA-256 + salt)
INSERT INTO users (username, email, password_hash, full_name) VALUES
('alice',   'alice@example.com',   '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8:salt1', 'Alice Johnson'),
('bob',     'bob@example.com',     '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8:salt2', 'Bob Smith'),
('charlie', 'charlie@example.com', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8:salt3', 'Charlie Brown');

-- Wallets with initial balances
INSERT INTO wallets (user_id, balance_cents) VALUES
(1, 10000000),
(2, 5000000),
(3, 7500000);

-- Sample items
INSERT INTO items (seller_id, name, description, brand, category, price_cents, status) VALUES
(1, 'MacBook Pro 16"',       'Apple MacBook Pro with M3 chip, 16GB RAM',             'Apple',   'Electronics', 249999, 'ACTIVE'),
(1, 'iPhone 15 Pro',         'Latest iPhone with A17 Pro chip',                      'Apple',   'Electronics', 119999, 'ACTIVE'),
(2, 'Samsung Galaxy S24',    'Samsung flagship with Snapdragon 8 Gen 3',             'Samsung', 'Electronics',  89999, 'ACTIVE'),
(2, 'Sony WH-1000XM5',      'Premium noise-cancelling wireless headphones',         'Sony',    'Audio',        34999, 'ACTIVE'),
(3, 'Nike Air Max 90',       'Classic Nike sneakers in white/black colorway',        'Nike',    'Shoes',        12999, 'ACTIVE'),
(3, 'Adidas Ultraboost 23',  'High-performance running shoes with Boost technology', 'Adidas',  'Shoes',        17999, 'ACTIVE'),
(1, 'Dell XPS 15',           'Dell premium laptop with OLED display',                'Dell',    'Electronics', 179999, 'ACTIVE'),
(2, 'Canon EOS R6 Mark II',  'Full-frame mirrorless camera body',                    'Canon',   'Photography', 249999, 'ACTIVE');

-- Inventory for each item
INSERT INTO inventory (item_id, seller_id, quantity, reserved) VALUES
(1, 1, 5,  0),
(2, 1, 10, 0),
(3, 2, 8,  0),
(4, 2, 15, 0),
(5, 3, 20, 0),
(6, 3, 12, 0),
(7, 1, 3,  0),
(8, 2, 6,  0);

-- Sample external store
INSERT INTO external_stores (store_name, api_key, contact_email) VALUES
('TechStore Online', 'ts-api-key-abc123', 'admin@techstore.com');
