package com.medkernel.engine.embed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class EmbedEngineServiceTest {

    private EmbedLaunchTokenRepository tokenRepo;
    private EmbedOriginWhitelistRepository originRepo;
    private AuditEventPublisher auditPublisher;
    private IsolatedAuditPublisher isolatedAudit;
    private EmbedEngineService service;

    @BeforeEach
    void setUp() {
        tokenRepo = mock(EmbedLaunchTokenRepository.class);
        originRepo = mock(EmbedOriginWhitelistRepository.class);
        auditPublisher = mock(AuditEventPublisher.class);
        isolatedAudit = mock(IsolatedAuditPublisher.class);
        service = new EmbedEngineService(tokenRepo, originRepo, auditPublisher, isolatedAudit);

        RequestContext.restore(new RequestContext.Snapshot("trace-1", OrgScope.tenant("tenant-1"), "user-1"));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void generateToken_SucceedsAndSavesUNUSEDToken() {
        EmbedLaunchTokenRequest req = new EmbedLaunchTokenRequest("user-1", "doctor", "P100", "E200", "OUTPATIENT", 60);
        when(tokenRepo.save(any(EmbedLaunchToken.class))).thenAnswer(inv -> inv.getArgument(0));

        EmbedLaunchTokenResponse res = service.generateToken(req);

        assertThat(res.token()).startsWith("tkn-");
        assertThat(res.embedUrl()).contains(res.token());
        assertThat(res.expiredAt()).isAfter(Instant.now());
        verify(tokenRepo).save(any(EmbedLaunchToken.class));
        verify(auditPublisher).publish(eq(AuditAction.CREATE), eq("embed_launch_token"), eq(res.token()), any());
    }

    @Test
    void validateAndExchange_UNUSEDToken_SucceedsAndAtomicallyLocksUSED() {
        String tokenVal = "tkn-123456";
        Instant expiredAt = Instant.now().plusSeconds(60);
        EmbedLaunchToken unused = new EmbedLaunchToken(
            1L, tokenVal, "tenant-1", "user-1", "doctor", "P100", "E200", "OUTPATIENT",
            "UNUSED", expiredAt, Instant.now(), "user-1", Instant.now(), "user-1", "trace-1"
        );

        when(tokenRepo.findByToken(tokenVal)).thenReturn(Optional.of(unused));
        when(tokenRepo.save(any(EmbedLaunchToken.class))).thenAnswer(inv -> inv.getArgument(0));

        EmbedLaunchContextResponse res = service.validateAndExchange(tokenVal, null);

        assertThat(res.active()).isTrue();
        assertThat(res.userId()).isEqualTo("user-1");
        assertThat(res.tenantId()).isEqualTo("tenant-1");
        assertThat(res.patientId()).isEqualTo("P100");

        verify(tokenRepo).save(any(EmbedLaunchToken.class));
        verify(auditPublisher).publish(eq(AuditAction.EXECUTE), eq("embed_launch_token"), eq(tokenVal), any());
    }

    @Test
    void validateAndExchange_AlreadyUSEDToken_ThrowsConflict() {
        String tokenVal = "tkn-123456";
        EmbedLaunchToken used = new EmbedLaunchToken(
            1L, tokenVal, "tenant-1", "user-1", "doctor", "P100", "E200", "OUTPATIENT",
            "USED", Instant.now().plusSeconds(60), Instant.now(), "user-1", Instant.now(), "user-1", "trace-1"
        );

        when(tokenRepo.findByToken(tokenVal)).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.validateAndExchange(tokenVal, null))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENG_EMBED_003);

        verify(isolatedAudit).publishInNewTx(any());
    }

    @Test
    void validateAndExchange_ExpiredToken_ThrowsExpiredAndSetsEXPIRED() {
        String tokenVal = "tkn-123456";
        EmbedLaunchToken unused = new EmbedLaunchToken(
            1L, tokenVal, "tenant-1", "user-1", "doctor", "P100", "E200", "OUTPATIENT",
            "UNUSED", Instant.now().minusSeconds(10), Instant.now().minusSeconds(100), "user-1", Instant.now().minusSeconds(100), "user-1", "trace-1"
        );

        when(tokenRepo.findByToken(tokenVal)).thenReturn(Optional.of(unused));
        when(tokenRepo.save(any(EmbedLaunchToken.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.validateAndExchange(tokenVal, null))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENG_EMBED_001);

        verify(tokenRepo).save(any(EmbedLaunchToken.class));
        verify(isolatedAudit).publishInNewTx(any());
    }

    @Test
    void validateAndExchange_OriginNotInWhitelist_ThrowsForbidden() {
        String tokenVal = "tkn-123456";
        EmbedLaunchToken unused = new EmbedLaunchToken(
            1L, tokenVal, "tenant-1", "user-1", "doctor", "P100", "E200", "OUTPATIENT",
            "UNUSED", Instant.now().plusSeconds(60), Instant.now(), "user-1", Instant.now(), "user-1", "trace-1"
        );

        when(tokenRepo.findByToken(tokenVal)).thenReturn(Optional.of(unused));
        when(originRepo.findByTenantIdAndOrigin("tenant-1", "https://unauthorized.domain.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndExchange(tokenVal, "https://unauthorized.domain.com"))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENG_EMBED_002);

        verify(isolatedAudit).publishInNewTx(any());
    }

    @Test
    void feedback_SucceedsAndPublishesAudit() {
        String tokenVal = "tkn-123456";
        EmbedLaunchToken unused = new EmbedLaunchToken(
            1L, tokenVal, "tenant-1", "user-1", "doctor", "P100", "E200", "OUTPATIENT",
            "UNUSED", Instant.now().plusSeconds(60), Instant.now(), "user-1", Instant.now(), "user-1", "trace-1"
        );

        when(tokenRepo.findByToken(tokenVal)).thenReturn(Optional.of(unused));
        EmbedFeedbackRequest req = new EmbedFeedbackRequest(tokenVal, "ACCEPT", "患者风险已确认，安排开医嘱");

        service.feedback(req);

        verify(auditPublisher).publish(eq(AuditAction.FEEDBACK), eq("embed_launch_token"), eq(tokenVal), any());
    }

    @Test
    void addAndGetOrigins_Succeeds() {
        when(originRepo.findByTenantIdAndOrigin("tenant-1", "https://his.hospital.com")).thenReturn(Optional.empty());
        when(originRepo.save(any(EmbedOriginWhitelist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(originRepo.findByTenantId("tenant-1")).thenReturn(List.of(
            new EmbedOriginWhitelist(1L, "tenant-1", "https://his.hospital.com", Instant.now(), "user-1", Instant.now(), "user-1")
        ));

        service.addOrigin(new EmbedOriginRequest("https://his.hospital.com"));
        List<String> list = service.getOrigins();

        assertThat(list).containsExactly("https://his.hospital.com");
        verify(originRepo).save(any(EmbedOriginWhitelist.class));
    }
}
