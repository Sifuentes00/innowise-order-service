-- Add email column to orders table
ALTER TABLE orders ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '';

-- Add index on email column for faster lookups
CREATE INDEX idx_orders_email ON orders(email);
