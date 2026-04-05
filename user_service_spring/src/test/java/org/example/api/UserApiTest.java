package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("users")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /users — 201 и тело с полями пользователя")
    void postUserReturnsCreatedAndBody() throws Exception {
        CreateUserDto dto = new CreateUserDto("Anna", "anna@gmail.com", 25);

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/*+json")))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.get("id").asLong() > 0);
        assertEquals("Anna", body.get("name").asText());
        assertEquals("anna@gmail.com", body.get("email").asText());
        assertEquals(25, body.get("age").asInt());
    }

    @Test
    @DisplayName("GET /users/{id} — 200, если пользователь есть")
    void getUserByIdReturnsOk() throws Exception {
        long id = createUserViaApi("Kiki", "kiki@gmail.com", 20);

        MvcResult result = mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(id, body.get("id").asLong());
        assertEquals("Kiki", body.get("name").asText());
        assertEquals("kiki@gmail.com", body.get("email").asText());
    }

    @Test
    @DisplayName("GET /users/{id} — 404, если пользователя нет")
    void getUserByIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}", 9_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users — 200 и HAL collection (_embedded)")
    void getAllUsersReturnsOkAndHalEmbedded() throws Exception {
        createUserViaApi("Asasa", "asasa@gmail.com", 18);
        createUserViaApi("Bvbv", "bvbv@gmail.com", 19);

        MvcResult result = mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode embedded = root.path("_embedded");
        assertTrue(embedded.isObject(), "Ожидался HAL с _embedded: " + root);

        JsonNode array = null;
        for (Iterator<String> it = embedded.fieldNames(); it.hasNext(); ) {
            JsonNode node = embedded.get(it.next());
            if (node != null && node.isArray()) {
                array = node;
                break;
            }
        }
        assertNotNull(array, "В _embedded должна быть коллекция DTO");
        assertEquals(2, array.size());
    }

    @Test
    @DisplayName("PUT /users/{id} — 200 и обновлённые поля")
    void putUserReturnsOkAndUpdatedFields() throws Exception {
        long id = createUserViaApi("Mila", "mila@gmail.com", 22);

        UpdateUserDto update = new UpdateUserDto("milala@gmail.com", 33);
        MvcResult result = mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(id, body.get("id").asLong());
        assertEquals("milala@gmail.com", body.get("email").asText());
        assertEquals(33, body.get("age").asInt());
    }

    @Test
    @DisplayName("DELETE /users/{id} — 204, затем GET того же id — 404")
    void deleteUserReturnsNoContentThenGetNotFound() throws Exception {
        long id = createUserViaApi("Kirieshka", "kirieshka@gmail.com", 40);

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        MvcResult notFound = mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode body = objectMapper.readTree(notFound.getResponse().getContentAsString());
        assertTrue(body.hasNonNull("error"));
    }

    @Test
    @DisplayName("POST /users с невалидным email — 400")
    void postUserWithInvalidEmailReturnsBadRequest() throws Exception {
        CreateUserDto dto = new CreateUserDto("Huan", "hhjjgg", 20);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    private long createUserViaApi(String name, String email, int age) throws Exception {
        CreateUserDto dto = new CreateUserDto(name, email, age);
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
