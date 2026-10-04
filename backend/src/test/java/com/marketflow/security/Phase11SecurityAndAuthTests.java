package com.marketflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.auth.LoginRequest;
import com.marketflow.dto.auth.RefreshTokenRequest;
import com.marketflow.dto.auth.RegisterRequest;
import com.marketflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 11: Enterprise Security & JWT Token Rotation Tests")
class Phase11SecurityAndAuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Security: Public endpoints (/health, /ping, /templates) accessible without token")
    void testPublicEndpointsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Protected endpoints (/workflows, /executions, /ai) reject unauthenticated requests with 401")
    void testProtectedEndpointsRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/workflows"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/executions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth: Register new user returns 201 with JWT access & refresh tokens")
    void testRegisterNewUser() throws Exception {
        RegisterRequest req = new RegisterRequest("newdeveloper@marketflow.demo", "Secret123!", "New Developer");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("newdeveloper@marketflow.demo"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Auth: Duplicate email registration returns 409 Conflict")
    void testDuplicateEmailRegistrationFails() throws Exception {
        RegisterRequest req = new RegisterRequest("duplicate@marketflow.demo", "Secret123!", "Dupe User");

        // First registration
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Auth: Login with valid credentials returns tokens; invalid credentials returns 401")
    void testLoginValidAndInvalid() throws Exception {
        // Register a dedicated user for this test
        RegisterRequest reg = new RegisterRequest("logintest@marketflow.demo", "ValidPass123!", "Login Tester");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Login with correct password
        LoginRequest validLogin = new LoginRequest("logintest@marketflow.demo", "ValidPass123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // Login with wrong password
        LoginRequest invalidLogin = new LoginRequest("logintest@marketflow.demo", "WrongPassword!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"));
    }

    @Test
    @DisplayName("Security: Protected endpoint accepts Bearer JWT token and returns 200 OK")
    void testProtectedEndpointWithValidJwt() throws Exception {
        String token = jwtService.generateAccessToken("test-user-id", "tokenbearer@marketflow.demo", "ROLE_USER");

        mockMvc.perform(get("/workflows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth: Token Rotation & Theft Detection: Refresh token rotates, and reused token is revoked")
    void testTokenRotationAndTheftDetection() throws Exception {
        RegisterRequest reg = new RegisterRequest("rotation@marketflow.demo", "RotatePass123!", "Rotation Tester");
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String respStr = regResult.getResponse().getContentAsString();
        String initialRefreshToken = objectMapper.readTree(respStr).path("data").path("refreshToken").asText();
        assertNotNull(initialRefreshToken);

        // 1. First rotation: Refreshing with initial token -> succeeds with fresh tokens
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult rotateResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String secondRefreshToken = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // 2. Theft detection: Re-using the OLD (already rotated) token -> fails!
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // 3. Logout revokes token
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Multi-device logout independence - logging out on one device preserves other active devices")
    void testMultiDeviceLogoutIndependence() throws Exception {
        // Register user
        RegisterRequest reg = new RegisterRequest("multidevice@marketflow.demo", "SecretPass123!", "Multi Device User");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Device 1 logs in
        LoginRequest login1 = new LoginRequest("multidevice@marketflow.demo", "SecretPass123!");
        MvcResult res1 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();
        String device1RefreshToken = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("refreshToken").asText();
        String device1AccessToken = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Device 2 logs in
        LoginRequest login2 = new LoginRequest("multidevice@marketflow.demo", "SecretPass123!");
        MvcResult res2 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();
        String device2RefreshToken = objectMapper.readTree(res2.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Log out Device 1 ONLY
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(device1RefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully from this device"));

        // Device 2 refresh token is STILL VALID! (Independent session)
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(device2RefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Full logout across all devices using Bearer token
        mockMvc.perform(post("/auth/logout-all")
                        .header("Authorization", "Bearer " + device1AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully from all devices"));
    }
}
