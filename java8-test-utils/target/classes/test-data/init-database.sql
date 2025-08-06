-- Test database initialization script
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    stock_quantity INTEGER DEFAULT 0,
    category VARCHAR(50),
    active BOOLEAN DEFAULT TRUE
);

-- Insert test data
INSERT INTO users (username, email, status) VALUES 
    ('testuser1', 'test1@example.com', 'ACTIVE'),
    ('testuser2', 'test2@example.com', 'ACTIVE'),
    ('testuser3', 'test3@example.com', 'INACTIVE');

INSERT INTO products (product_name, price, stock_quantity, category) VALUES
    ('Test Product A', 29.99, 100, 'Electronics'),
    ('Test Product B', 19.99, 50, 'Books'),
    ('Test Product C', 49.99, 25, 'Clothing');

INSERT INTO orders (user_id, order_number, total_amount, order_status) VALUES
    (1, 'ORD-001', 29.99, 'COMPLETED'),
    (2, 'ORD-002', 19.99, 'PENDING'),
    (1, 'ORD-003', 49.99, 'SHIPPED');