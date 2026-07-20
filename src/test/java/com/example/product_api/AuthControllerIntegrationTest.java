package com.example.product_api;

import com.example.product_api.repository.RefreshTokenRepository;
import com.example.product_api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort; // Spring Boot 3+
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("email", "test@example.com");
        request.put("password", "test123456");

        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/register"), request, String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertNotNull(json);
        assertEquals("testuser", json.get("username").asText());
        assertEquals("test@example.com", json.get("email").asText());
        assertTrue(json.has("id"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        registerUser("alice", "alice@example.com", "password123");

        Map<String, String> loginRequest = Map.of(
                "username", "alice",
                "password", "password123"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), loginRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertNotNull(json);
        assertFalse(json.get("accessToken").asText().isEmpty());
        assertFalse(json.get("refreshToken").asText().isEmpty());

        // Verificar que el refresh token se guardó en BD
        String refreshToken = json.get("refreshToken").asText();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();
    }

    @Test
    void shouldRejectInvalidCredentials() {
        Map<String, String> loginRequest = Map.of(
                "username", "nonexistent",
                "password", "wrong"
        );

        try {
            restTemplate.postForEntity(url("/api/auth/login"), loginRequest, String.class);
            fail("Expected HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            // El cuerpo puede estar vacío, así que solo verificamos el código de estado
            System.out.println("Response body: " + e.getResponseBodyAsString()); // opcional
        }
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        registerUser("bob", "bob@example.com", "bob123456");
        String refreshToken = loginAndGetRefreshToken("bob", "bob123456");

        Map<String, String> refreshRequest = Map.of("refreshToken", refreshToken);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/refresh"), refreshRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertNotNull(json);
        assertFalse(json.get("accessToken").asText().isEmpty());
        assertFalse(json.get("refreshToken").asText().isEmpty());
        assertNotEquals(refreshToken, json.get("refreshToken").asText());

        // El refresh token antiguo ya no debe existir
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void shouldAccessProtectedEndpointWithToken() throws Exception {
        registerUser("admin", "admin@example.com", "admin123");
        String accessToken = loginAndGetAccessToken("admin", "admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url("/api/products"), HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Métodos auxiliares

    private void registerUser(String username, String email, String password) {
        Map<String, String> request = Map.of("username", username, "email", email, "password", password);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/register"), request, String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private String loginAndGetRefreshToken(String username, String password) throws Exception {
        Map<String, String> loginRequest = Map.of("username", username, "password", password);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), loginRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.get("refreshToken").asText();
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        Map<String, String> loginRequest = Map.of("username", username, "password", password);
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), loginRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.get("accessToken").asText();
    }
}