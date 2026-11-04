package com.tizvagt.api.catalog;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tizvagt.api.TestcontainersConfiguration;
import com.tizvagt.api.security.JwtService;
import com.tizvagt.api.user.Role;
import com.tizvagt.api.user.User;
import com.tizvagt.api.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.storage.upload-dir=build/test-uploads")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JwtService jwtService;

    @Test
    void listsProductsWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.content[0].category.name").exists());
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/products").param("search", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Effective Java"));
    }

    @Test
    void filtersByCategoryAndPrice() throws Exception {
        mockMvc.perform(get("/api/products").param("categoryId", "1").param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].category.id", everyItem(is(1))))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void paginatesAndSorts() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "5").param("page", "0").param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Cotton T-Shirt"));
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "hacker"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProductReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name").value("Books"));
    }

    @Test
    void adminCanUploadImageAndItIsServed() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();

        String body = mockMvc.perform(multipart("/api/products/{id}/image", productId)
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/")))
                .andReturn().getResponse().getContentAsString();

        String imageUrl = JsonPath.read(body, "$.imageUrl");
        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotUploadImage() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();

        mockMvc.perform(multipart("/api/products/{id}/image", productId)
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", bearer(Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadRequiresLogin() throws Exception {
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonImageUploadIsRejected() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();

        mockMvc.perform(multipart("/api/products/{id}/image", productId)
                        .file(new MockMultipartFile("file", "notes.png", "image/png", "hello".getBytes()))
                        .header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    private String bearer(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, "not-used", "Test " + role, role));
        return "Bearer " + jwtService.generateToken(email, role);
    }
}
