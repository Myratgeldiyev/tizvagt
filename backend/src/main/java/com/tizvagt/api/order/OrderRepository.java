package com.tizvagt.api.order;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Items are loaded in batches thanks to hibernate.default_batch_fetch_size.
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // Filtering by user id too means a user can never load someone else's order (prevents IDOR).
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
