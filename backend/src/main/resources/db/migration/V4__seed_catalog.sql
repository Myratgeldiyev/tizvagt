-- Sample catalog so the app has something to show right away.
INSERT INTO categories (id, name, slug) VALUES
    (1, 'Electronics', 'electronics'),
    (2, 'Clothing', 'clothing'),
    (3, 'Home & Kitchen', 'home-kitchen'),
    (4, 'Books', 'books'),
    (5, 'Sports', 'sports');

INSERT INTO products (name, description, price, stock, category_id, created_at) VALUES
    ('Wireless Headphones', 'Over-ear Bluetooth headphones with active noise cancelling and 30h battery.', 129.99, 25, 1, NOW(6)),
    ('Smartphone Stand', 'Adjustable aluminium stand for phones and small tablets.', 19.90, 100, 1, NOW(6)),
    ('Mechanical Keyboard', 'Compact 75% keyboard with hot-swappable switches and RGB backlight.', 89.00, 40, 1, NOW(6)),
    ('USB-C Charger 65W', 'GaN fast charger with two USB-C ports and one USB-A port.', 39.50, 60, 1, NOW(6)),
    ('Cotton T-Shirt', 'Classic fit t-shirt made from 100% organic cotton.', 14.99, 200, 2, NOW(6)),
    ('Denim Jacket', 'Light-wash denim jacket with button front.', 59.90, 30, 2, NOW(6)),
    ('Running Shoes', 'Lightweight running shoes with breathable mesh upper.', 74.99, 50, 5, NOW(6)),
    ('Yoga Mat', 'Non-slip 6mm yoga mat with carrying strap.', 24.99, 80, 5, NOW(6)),
    ('Chef Knife', '8-inch stainless steel chef knife with ergonomic handle.', 34.99, 45, 3, NOW(6)),
    ('French Press', '1L glass French press coffee maker.', 22.50, 70, 3, NOW(6)),
    ('Clean Code', 'A Handbook of Agile Software Craftsmanship by Robert C. Martin.', 32.00, 35, 4, NOW(6)),
    ('Effective Java', 'Best practices for the Java platform by Joshua Bloch.', 45.00, 20, 4, NOW(6));
