package com.tizvagt.api.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // @EntityGraph loads the category in the same SQL query (JOIN),
    // which avoids the N+1 query problem when listing products.
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    /**
     * Atomic "check and decrease" in a single SQL statement. Returns 1 if the stock was reduced,
     * 0 if there wasn't enough. The database row lock makes this safe when many users buy at once.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
