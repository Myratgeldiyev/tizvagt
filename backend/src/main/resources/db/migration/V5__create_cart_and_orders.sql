CREATE TABLE cart_items (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);

CREATE TABLE orders (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    user_id           BIGINT         NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    total_amount      DECIMAL(12, 2) NOT NULL,
    payment_reference VARCHAR(100),
    created_at        DATETIME(6)    NOT NULL,
    paid_at           DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_orders_user_created (user_id, created_at)
);

-- Name and price are copied at checkout, so later product changes don't rewrite order history.
CREATE TABLE order_items (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    quantity     INT            NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0)
);
