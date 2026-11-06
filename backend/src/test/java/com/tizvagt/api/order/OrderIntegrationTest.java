package com.tizvagt.api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tizvagt.api.TestcontainersConfiguration;
import com.tizvagt.api.catalog.CategoryRepository;
import com.tizvagt.api.catalog.Product;
import com.tizvagt.api.catalog.ProductRepository;
import com.tizvagt.api.security.JwtService;
import com.tizvagt.api.user.Role;
import com.tizvagt.api.user.User;
import com.tizvagt.api.user.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void cartAddUpdateRemove() throws Exception {
        String token = newUserToken();
        Product mug = newProduct("10.00", 10);

        addToCart(token, mug.getId(), 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(20.00));

        // Adding the same product again increases the quantity instead of adding a new line.
        addToCart(token, mug.getId(), 1)
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        mockMvc.perform(patch("/api/cart/items/{id}", mug.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(50.00));

        mockMvc.perform(delete("/api/cart/items/{id}", mug.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void cannotAddMoreThanStock() throws Exception {
        String token = newUserToken();
        Product rare = newProduct("99.00", 2);

        addToCart(token, rare.getId(), 3).andExpect(status().isConflict());
    }

    @Test
    void cartRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutCreatesPaidOrderDecreasesStockAndEmptiesCart() throws Exception {
        String token = newUserToken();
        Product book = newProduct("12.50", 10);
        Product pen = newProduct("1.25", 100);
        addToCart(token, book.getId(), 2);
        addToCart(token, pen.getId(), 4);

        mockMvc.perform(post("/api/orders").header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(30.00))
                .andExpect(jsonPath("$.paymentReference", startsWith("MOCK-")))
                .andExpect(jsonPath("$.items", hasSize(2)));

        assertThat(stockOf(book)).isEqualTo(8);
        assertThat(stockOf(pen)).isEqualTo(96);
        mockMvc.perform(get("/api/cart").header("Authorization", token))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void orderKeepsPriceFromCheckoutTime() throws Exception {
        String token = newUserToken();
        Product lamp = newProduct("40.00", 5);
        addToCart(token, lamp.getId(), 1);
        long orderId = checkout(token);

        jdbc.update("UPDATE products SET price = 999.99 WHERE id = ?", lamp.getId());

        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(40.00))
                .andExpect(jsonPath("$.totalAmount").value(40.00));
    }

    @Test
    void checkoutWithEmptyCartIsRejected() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", newUserToken()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void failedCheckoutRollsBackEverything() throws Exception {
        String token = newUserToken();
        Product first = newProduct("5.00", 10);
        Product second = newProduct("5.00", 10);
        addToCart(token, first.getId(), 3);
        addToCart(token, second.getId(), 3);
        // Someone else bought the second product in the meantime.
        jdbc.update("UPDATE products SET stock = 1 WHERE id = ?", second.getId());

        mockMvc.perform(post("/api/orders").header("Authorization", token))
                .andExpect(status().isConflict());

        // The first product's stock was decreased inside the transaction, then rolled back.
        assertThat(stockOf(first)).isEqualTo(10);
        mockMvc.perform(get("/api/cart").header("Authorization", token))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mockMvc.perform(get("/api/orders").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void usersCannotSeeOtherUsersOrders() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        Product item = newProduct("3.00", 10);
        addToCart(alice, item.getId(), 1);
        long aliceOrder = checkout(alice);

        mockMvc.perform(get("/api/orders/{id}", aliceOrder).header("Authorization", bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders/{id}/cancel", aliceOrder).header("Authorization", bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelRestoresStockAndCannotBeDoneTwice() throws Exception {
        String token = newUserToken();
        Product item = newProduct("8.00", 5);
        addToCart(token, item.getId(), 2);
        long orderId = checkout(token);
        assertThat(stockOf(item)).isEqualTo(3);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(item)).isEqualTo(5);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", token))
                .andExpect(status().isUnprocessableContent());
        assertThat(stockOf(item)).isEqualTo(5);
    }

    @Test
    void listsOwnOrdersNewestFirst() throws Exception {
        String token = newUserToken();
        Product item = newProduct("2.00", 50);
        addToCart(token, item.getId(), 1);
        long first = checkout(token);
        addToCart(token, item.getId(), 1);
        long second = checkout(token);

        mockMvc.perform(get("/api/orders").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[1].id").value(first));
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions addToCart(String token, Long productId, int qty)
            throws Exception {
        return mockMvc.perform(post("/api/cart/items").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": %d, \"quantity\": %d}".formatted(productId, qty)));
    }

    private long checkout(String token) throws Exception {
        String body = mockMvc.perform(post("/api/orders").header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String newUserToken() {
        String email = "buyer-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, "not-used", "Buyer", Role.USER));
        return "Bearer " + jwtService.generateToken(email, Role.USER);
    }

    private Product newProduct(String price, int stock) {
        return productRepository.save(new Product("Test product " + UUID.randomUUID(), "desc",
                new BigDecimal(price), stock, categoryRepository.getReferenceById(1L)));
    }

    private int stockOf(Product product) {
        return jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, product.getId());
    }
}
