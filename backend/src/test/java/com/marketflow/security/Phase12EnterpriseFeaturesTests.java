package com.marketflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.dto.*;
import com.marketflow.dto.auth.LoginRequest;
import com.marketflow.dto.auth.RefreshTokenRequest;
import com.marketflow.dto.auth.RegisterRequest;
import com.marketflow.engine.ExecutionContext;
import com.marketflow.engine.NodeExecutionResult;
import com.marketflow.engine.handler.ActionHandler;
import com.marketflow.model.enums.WorkflowStatus;
import com.marketflow.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 12: Enterprise Security Hardening & Advanced Backend Features")
class Phase12EnterpriseFeaturesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ActionHandler actionHandler;

    @Autowired
    private SsrfValidator ssrfValidator;

    @Test
    @DisplayName("IDOR Defense: User B cannot read, update, or delete User A's private workflow (403 Forbidden)")
    void testMultiTenantIdorDefense() throws Exception {
        // 1. Register User A
        RegisterRequest userAReq = new RegisterRequest("usera@marketflow.demo", "Password123!", "User Alpha");
        MvcResult regAResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userAReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String userAToken = objectMapper.readTree(regAResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 2. User A creates a private workflow
        CreateWorkflowRequest createWf = new CreateWorkflowRequest(
                "Alpha Secret Pipeline",
                "Confidential marketing campaigns",
                List.of(
                        new NodeDto("trig", "manual_trigger", new PositionDto(100, 100), Map.of("label", "Manual Trigger")),
                        new NodeDto("act", "action_email", new PositionDto(100, 250), Map.of("label", "Email Step", "recipient", "lead@company.com"))
                ),
                List.of(new EdgeDto("e1", "trig", "act"))
        );

        MvcResult createResult = mockMvc.perform(post("/workflows")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWf)))
                .andExpect(status().isCreated())
                .andReturn();
        String alphaWorkflowId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asText();

        // 3. Register User B
        RegisterRequest userBReq = new RegisterRequest("userb@marketflow.demo", "Password123!", "User Bravo");
        MvcResult regBResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String userBToken = objectMapper.readTree(regBResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 4. User B attempts IDOR READ on User A's workflow -> 403 Forbidden!
        mockMvc.perform(get("/workflows/" + alphaWorkflowId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        // 5. User B attempts IDOR UPDATE on User A's workflow -> 403 Forbidden!
        UpdateWorkflowRequest updateWf = new UpdateWorkflowRequest();
        updateWf.setName("Hacked Alpha Workflow");
        mockMvc.perform(put("/workflows/" + alphaWorkflowId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateWf)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        // 6. User B attempts IDOR DELETE on User A's workflow -> 403 Forbidden!
        mockMvc.perform(delete("/workflows/" + alphaWorkflowId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        // 7. User A can successfully read their own workflow -> 200 OK!
        mockMvc.perform(get("/workflows/" + alphaWorkflowId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha Secret Pipeline"));
    }

    @Test
    @DisplayName("Workflow Import & Export: Workflow exports to bundle with SHA-256 and imports cleanly")
    void testWorkflowExportAndImport() throws Exception {
        // Register user
        RegisterRequest userReq = new RegisterRequest("exportuser@marketflow.demo", "Password123!", "Exporter");
        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Create workflow
        CreateWorkflowRequest createWf = new CreateWorkflowRequest(
                "Exportable Pipeline",
                "Workflow to test export/import roundtrip",
                List.of(
                        new NodeDto("trig_1", "manual_trigger", new PositionDto(50, 50), Map.of("label", "Lead Trigger")),
                        new NodeDto("slack_1", "action_slack", new PositionDto(50, 200), Map.of("label", "Alert Team", "channel", "#sales"))
                ),
                List.of(new EdgeDto("edge_1", "trig_1", "slack_1"))
        );

        MvcResult createRes = mockMvc.perform(post("/workflows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWf)))
                .andExpect(status().isCreated())
                .andReturn();
        String wfId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("id").asText();

        // Export workflow
        MvcResult exportRes = mockMvc.perform(get("/workflows/" + wfId + "/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1.0"))
                .andExpect(jsonPath("$.name").value("Exportable Pipeline"))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges", hasSize(1)))
                .andExpect(jsonPath("$.checksum").isNotEmpty())
                .andReturn();

        WorkflowExportDto exportBundle = objectMapper.readValue(exportRes.getResponse().getContentAsString(), WorkflowExportDto.class);
        exportBundle.setName("Imported Cloned Pipeline");

        // Import workflow bundle
        mockMvc.perform(post("/workflows/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportBundle)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Imported Cloned Pipeline"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges", hasSize(1)));
    }

    @Test
    @DisplayName("SSRF Protection: Outbound HTTP actions to localhost, loopback, and metadata IPs are blocked")
    void testSsrfProtectionGuards() {
        // Loopback 127.0.0.1
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://127.0.0.1:8080/admin"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://localhost:3000/api"));

        // AWS/GCP/Azure Cloud Metadata IP
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://169.254.169.254/latest/meta-data/"));

        // Private Site-Local addresses
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://10.0.0.5/secrets"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://192.168.1.1/router"));

        // Forbidden non-HTTP protocols
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("file:///etc/passwd"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("ftp://internal.server/data"));

        // Node execution check
        ExecutionContext context = new ExecutionContext("test_exec", "test_wf", Map.of());
        NodeDto ssrfNode = new NodeDto(
                "http_node",
                "action_http",
                new PositionDto(0, 0),
                Map.of("url", "http://127.0.0.1:5432/postgres", "method", "POST")
        );
        NodeExecutionResult result = actionHandler.execute(ssrfNode, context);
        assertFalse(result.isSuccess(), "SSRF attack node should fail execution");
        assertTrue(result.getErrorMessage().contains("SSRF"), "Error message should mention SSRF violation");
    }

    @Test
    @DisplayName("Multi-Device Logout: Logging out Device 1 deletes only Device 1's token; Device 2 remains active")
    void testIndependentDeviceLogoutDetailed() throws Exception {
        RegisterRequest userReq = new RegisterRequest("twodevices@marketflow.demo", "Password123!", "Two Devices");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated());

        // Device 1 login (e.g. Chrome Laptop)
        MvcResult dev1Result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("twodevices@marketflow.demo", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String dev1Refresh = objectMapper.readTree(dev1Result.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Device 2 login (e.g. Safari Mobile)
        MvcResult dev2Result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("twodevices@marketflow.demo", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String dev2Refresh = objectMapper.readTree(dev2Result.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Logout Device 1
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(dev1Refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully from this device"));

        // Device 1 cannot refresh anymore (token removed)
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(dev1Refresh))))
                .andExpect(status().isUnauthorized());

        // Device 2 CAN STILL REFRESH and continue working!
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(dev2Refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
}
