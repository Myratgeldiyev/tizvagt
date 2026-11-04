CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(2000)  NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT            NOT NULL,
    image_url   VARCHAR(500),
    category_id BIGINT         NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0),
    INDEX idx_products_name (name)
);
