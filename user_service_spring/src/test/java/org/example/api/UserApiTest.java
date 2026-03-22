package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.AppConfig;
import org.example.config.JpaConfig;
import org.example.config.WebConfig;
import org.example.dto.CreateUserDto;
import org.example.dto.UpdateUserDto;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class UserApiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("users")
            .withUsername("postgres")
            .withPassword("postgres");

    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setupSpringAndMockMvc() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");

        context = new AnnotationConfigWebApplicationContext();
        context.register(AppConfig.class, JpaConfig.class, WebConfig.class);
        context.setServletContext(new MockServletContext());
        context.refresh();

        WebApplicationContext webApplicationContext = context;
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void clearUsers() {
        context.getBean(UserRepository.class).deleteAll();
    }

    @Test
    @DisplayName("POST /users — 201 и тело с полями пользователя")
    void postUserReturnsCreatedAndBody() throws Exception {
        CreateUserDto dto = new CreateUserDto("Anna", "anna@gmail.com", 25);

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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
    @DisplayName("GET /users — 200 и список пользователей")
    void getAllUsersReturnsOkAndArray() throws Exception {
        createUserViaApi("Asasa", "asasa@gmail.com", 18);
        createUserViaApi("Bvbv", "bvbv@gmail.com", 19);

        MvcResult result = mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(array.isArray());
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
