package com.motionnblur.senkron_backend.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedGetEndpoint_returnsUnauthorizedWithoutCredentials() throws Exception {
        mockMvc.perform(get("/channels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedPostEndpoint_returnsForbiddenWithoutCsrf() throws Exception {
        mockMvc.perform(post("/channels"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsHeaders_arePresentForAllowedOrigin() throws Exception {
        mockMvc.perform(get("/channels")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsRequest_fromDisallowedOrigin_isRejected() throws Exception {
        mockMvc.perform(get("/channels")
                        .header("Origin", "http://evil.example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsHeaders_arePresentForAllowedOriginOnPermittedSwaggerEndpoint() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isFound())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsRequest_fromDisallowedOriginOnSwaggerEndpoint_isRejected() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")
                        .header("Origin", "http://evil.example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerEndpoint_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound());
    }

    @Test
    void oauth2Endpoint_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound());
    }
}
