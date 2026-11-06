package com.tizvagt.api.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.tizvagt.api.TestcontainersConfiguration;
import com.tizvagt.api.cart.CartService;
import com.tizvagt.api.catalog.CategoryRepository;
import com.tizvagt.api.catalog.Product;
import com.tizvagt.api.catalog.ProductRepository;
import com.tizvagt.api.common.InsufficientStockException;
import com.tizvagt.api.user.Role;
import com.tizvagt.api.user.User;
import com.tizvagt.api.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Many users try to buy the last items at exactly the same time.
 * Without the atomic stock update, stock could go negative (overselling).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderConcurrencyTest {

    private static final int BUYERS = 10;
    private static final int STOCK = 3;

    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void neverSellsMoreThanStock() throws Exception {
        Product product = productRepository.save(new Product("Limited edition " + UUID.randomUUID(), "desc",
                new BigDecimal("100.00"), STOCK, categoryRepository.getReferenceById(1L)));

        List<Long> buyers = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            User user = userRepository.save(new User("rush-" + UUID.randomUUID() + "@example.com", "x", "Buyer", Role.USER));
            cartService.addItem(user.getId(), product.getId(), 1);
            buyers.add(user.getId());
        }

        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Long buyer : buyers) {
                futures.add(pool.submit(() -> {
                    startSignal.await(); // all threads start at the same moment
                    try {
                        orderService.checkout(buyer);
                        succeeded.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        outOfStock.incrementAndGet();
                    }
                    return null;
                }));
            }
            startSignal.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(succeeded.get()).isEqualTo(STOCK);
        assertThat(outOfStock.get()).isEqualTo(BUYERS - STOCK);
        Integer finalStock = jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, product.getId());
        assertThat(finalStock).isZero();
    }
}
