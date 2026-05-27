package com.medkernel.engine.embed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.context.RequestContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EmbedEngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbedEngineService service;

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    private static final String LAUNCH_TOKEN_BODY = """
        {
          "userId": "DOCTOR-001",
          "roleCode": "doctor",
          "patientId": "P1001",
          "encounterId": "E2001",
          "triggerPoint": "ORDER_SIGN",
          "expireSeconds": 60
        }
        """;

    private static final String FEEDBACK_BODY = """
        {
          "token": "tkn-123456",
          "actionType": "ACCEPT",
          "reason": "已采纳抗凝建议"
        }
        """;

    private static final String ORIGIN_BODY = """
        {
          "origin": "https://his.hospital.com"
        }
        """;

    @Test
    void generateToken_ReturnsOkWithResponse() throws Exception {
        EmbedLaunchTokenResponse mockResponse = new EmbedLaunchTokenResponse(
            "tkn-123456", Instant.now().plusSeconds(60), "/embed/launch?token=tkn-123456"
        );
        when(service.generateToken(any(EmbedLaunchTokenRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/engine/embed/launch-tokens")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("medical-affairs")))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEDICAL_AFFAIRS")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(LAUNCH_TOKEN_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").value("tkn-123456"))
            .andExpect(jsonPath("$.data.embedUrl").value("/embed/launch?token=tkn-123456"));

        verify(service).generateToken(any(EmbedLaunchTokenRequest.class));
    }

    @Test
    void validateAndExchange_ReturnsOkWithContext() throws Exception {
        EmbedLaunchContextResponse mockResponse = new EmbedLaunchContextResponse(
            "DOCTOR-001", "doctor", "tenant-1", "P1001", "E2001", "ORDER_SIGN", true, "trace-123"
        );
        when(service.validateAndExchange(eq("tkn-123456"), eq("https://his.hospital.com"))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/engine/embed/launch")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("doctor")))
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .header("Origin", "https://his.hospital.com")
                .param("token", "tkn-123456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value("DOCTOR-001"))
            .andExpect(jsonPath("$.data.patientId").value("P1001"))
            .andExpect(jsonPath("$.data.active").value(true));

        verify(service).validateAndExchange(eq("tkn-123456"), eq("https://his.hospital.com"));
    }

    @Test
    void feedback_ReturnsOk() throws Exception {
        doNothing().when(service).feedback(any(EmbedFeedbackRequest.class));

        mockMvc.perform(post("/api/v1/engine/embed/feedback")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("doctor")))
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(FEEDBACK_BODY))
            .andExpect(status().isOk());

        verify(service).feedback(any(EmbedFeedbackRequest.class));
    }

    @Test
    void addOrigin_ReturnsOk() throws Exception {
        doNothing().when(service).addOrigin(any(EmbedOriginRequest.class));

        mockMvc.perform(post("/api/v1/engine/embed/origins")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("it-ops")))
                    .authorities(new SimpleGrantedAuthority("ROLE_IT_OPS")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ORIGIN_BODY))
            .andExpect(status().isOk());

        verify(service).addOrigin(any(EmbedOriginRequest.class));
    }

    @Test
    void getOrigins_ReturnsList() throws Exception {
        when(service.getOrigins()).thenReturn(List.of("https://his.hospital.com"));

        mockMvc.perform(get("/api/v1/engine/embed/origins")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("doctor")))
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0]").value("https://his.hospital.com"));

        verify(service).getOrigins();
    }
}
