package com.medkernel.engine.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.TestPropertySource;

@DataJdbcTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:recommendation-repo-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class RecommendationRepositoryTest {

    @Autowired RecommendationTriggerRepository triggers;
    @Autowired RecommendationCardRepository cards;
    @Autowired RecommendationSourceRepository sources;
    @Autowired RecommendationFeedbackRepository feedback;
    @Autowired RecommendationFatigueSignalRepository fatigueSignals;

    @AfterEach
    void wipe() {
        fatigueSignals.deleteAll();
        feedback.deleteAll();
        sources.deleteAll();
        cards.deleteAll();
        triggers.deleteAll();
    }

    @Test
    void persistsRecommendationRuntimeFacts() {
        String triggerId = "rt-" + UUID.randomUUID();
        String cardId = "rc-" + UUID.randomUUID();

        RecommendationTrigger savedTrigger = triggers.save(sampleTrigger(triggerId, "tenant-A"));
        RecommendationCard savedCard = cards.save(sampleCard(cardId, "tenant-A", triggerId));
        RecommendationSource savedSource = sources.save(sampleSource("rs-" + UUID.randomUUID(), "tenant-A", cardId));
        RecommendationFeedback savedFeedback = feedback.save(sampleFeedback("rf-" + UUID.randomUUID(), "tenant-A", cardId));
        RecommendationFatigueSignal savedSignal =
            fatigueSignals.save(sampleFatigueSignal("rfs-" + UUID.randomUUID(), "tenant-A", triggerId, cardId));

        assertThat(savedTrigger.id()).isNotNull();
        assertThat(savedCard.id()).isNotNull();
        assertThat(savedSource.id()).isNotNull();
        assertThat(savedFeedback.id()).isNotNull();
        assertThat(savedSignal.id()).isNotNull();

        assertThat(triggers.findByTriggerIdAndTenantId(triggerId, "tenant-A")).isPresent();
        assertThat(cards.findByTriggerIdAndTenantIdOrderByCreatedAtAsc(triggerId, "tenant-A"))
            .extracting(RecommendationCard::cardCode)
            .containsExactly("CARD.ANTICOAG");
        assertThat(sources.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, "tenant-A"))
            .extracting(RecommendationSource::sourceType)
            .containsExactly(RecommendationSourceType.RULE);
        assertThat(feedback.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, "tenant-A"))
            .extracting(RecommendationFeedback::feedbackType)
            .containsExactly(RecommendationFeedbackType.ACCEPT);
        assertThat(fatigueSignals.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, "tenant-A"))
            .extracting(RecommendationFatigueSignal::signalType)
            .containsExactly(RecommendationFatigueSignalType.ACCEPTED);
    }

    @Test
    void repositoryQueriesDoNotLeakAcrossTenants() {
        String triggerId = "rt-" + UUID.randomUUID();
        triggers.save(sampleTrigger(triggerId, "tenant-A"));

        Optional<RecommendationTrigger> wrongTenant = triggers.findByTriggerIdAndTenantId(triggerId, "tenant-B");

        assertThat(wrongTenant).isEmpty();
    }

    private RecommendationTrigger sampleTrigger(String triggerId, String tenantId) {
        Instant now = Instant.now();
        return new RecommendationTrigger(
            null, triggerId, tenantId, "TRG." + triggerId, "ORDER_SIGN",
            "event-1", "snapshot-1", "patient-1", "enc-1", "pathway-1",
            "WARD_ORDER", "1.0.0", "sha256:trigger", RecommendationTriggerStatus.EVALUATED,
            null, now, now, "tester", now, "tester", "trace-recommendation");
    }

    private RecommendationCard sampleCard(String cardId, String tenantId, String triggerId) {
        Instant now = Instant.now();
        return new RecommendationCard(
            null, cardId, tenantId, triggerId, "CARD.ANTICOAG", RecommendationCardType.MEDICATION,
            "抗凝用药风险提醒", "患者当前医嘱满足抗凝风险规则", "请确认出血风险评估",
            RecommendationRiskLevel.HIGH, RecommendationInterruptLevel.WEAK_INTERRUPTIVE,
            RecommendationCardStatus.PENDING, true, false,
            "来源：抗凝用药规则 v1", "{\"reason\":\"规则命中\"}",
            "WARD_ORDER:ANTICOAG", now.plusSeconds(3600),
            now, "tester", now, "tester", "trace-recommendation");
    }

    private RecommendationSource sampleSource(String sourceId, String tenantId, String cardId) {
        Instant now = Instant.now();
        return new RecommendationSource(
            null, sourceId, tenantId, cardId, RecommendationSourceType.RULE,
            "rule-1", "v1", "抗凝用药规则", "§2.1",
            "sha256:source", "规则命中抗凝药品类别",
            now, "tester", now, "tester", "trace-recommendation");
    }

    private RecommendationFeedback sampleFeedback(String feedbackId, String tenantId, String cardId) {
        Instant now = Instant.now();
        return new RecommendationFeedback(
            null, feedbackId, tenantId, cardId, RecommendationFeedbackType.ACCEPT,
            "CONFIRMED", "已完成出血风险评估", "doctor-1", "DOCTOR",
            now, "doctor-1", now, "doctor-1", "trace-recommendation");
    }

    private RecommendationFatigueSignal sampleFatigueSignal(
            String signalId, String tenantId, String triggerId, String cardId) {
        Instant now = Instant.now();
        return new RecommendationFatigueSignal(
            null, signalId, tenantId, triggerId, cardId, "WARD_ORDER:ANTICOAG",
            "patient-1", "enc-1", "doctor-1", RecommendationFatigueSignalType.ACCEPTED,
            1, now.minusSeconds(300), now, "doctor-1", now, "doctor-1", "trace-recommendation");
    }
}
