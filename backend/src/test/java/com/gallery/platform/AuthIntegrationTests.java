package com.gallery.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registerLoginAndAccessProtectedEndpoint() throws Exception {
        String email = "admin@example.com";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "name", "Admin User",
                "password", "password123"
        ));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.admin.email").value(email))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDevices").value(0));

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void protectedEndpointsRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
    }
}
