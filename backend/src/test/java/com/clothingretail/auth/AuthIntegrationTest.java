package com.clothingretail.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers, end to end against the real security filter chain and an H2
 * database (test profile): register -> login -> access a protected endpoint
 * with the issued token, and that admin login rejects a CUSTOMER-role
 * account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerThenLoginThenAccessProtectedEndpointSucceeds() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
                {"fullName":"Test Customer","email":"%s","mobileNumber":"9876543210","password":"Password123!"}
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.roles[0]", is("CUSTOMER")))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String registerAccessToken = registerJson.get("accessToken").asText();

        // The access token from register should already work against a protected endpoint.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + registerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)));

        // Independently, login with the same credentials should also succeed and yield a working token.
        String loginBody = """
                {"email":"%s","password":"Password123!"}
                """.formatted(email);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email", is(email)))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String loginAccessToken = loginJson.get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + loginAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)));
    }

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminLoginRejectsCustomerAccount() throws Exception {
        String email = "not-an-admin-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
                {"fullName":"Plain Customer","email":"%s","mobileNumber":"9876500000","password":"Password123!"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        String adminLoginBody = """
                {"email":"%s","password":"Password123!"}
                """.formatted(email);

        // The account is valid and the password is correct, but it has no ADMIN/SUPER_ADMIN
        // role, so admin login must still be refused.
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapSuperAdminCanLogInThroughAdminLogin() throws Exception {
        // AdminBootstrapRunner seeds this account on startup from the test profile's
        // app.admin-bootstrap.* config (see application.yml).
        String adminLoginBody = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles", org.hamcrest.Matchers.hasItem("SUPER_ADMIN")));
    }
}
