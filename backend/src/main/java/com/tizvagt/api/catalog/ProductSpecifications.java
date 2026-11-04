package com.tizvagt.api.catalog;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small, reusable filter pieces. The service combines only the ones the client asked for.
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    static Specification<Product> nameContains(String search) {
        String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    static Specification<Product> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    // So that a search for "50%" looks for the text "50%" instead of treating % as a wildcard.
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
