package com.medkernel.engine.pathway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

class PathwayProgressorTest {

    private final PathwayProgressor progressor = new PathwayProgressor();

    @Test
    void followsDefaultEdgeWhenNodeCompletes() {
        PathwayProgressDecision decision = progressor.advance(new PathwayProgressCommand(
            graph(), "ASSESS", PathwayAdvanceEventType.COMPLETE, null));

        assertThat(decision.previousNodeCode()).isEqualTo("ASSESS");
        assertThat(decision.nextNodeCode()).isEqualTo("LAB");
        assertThat(decision.status()).isEqualTo(PatientPathwayStatus.NODE_EXECUTING);
    }

    @Test
    void respectsExplicitTargetNodeWhenItIsReachable() {
        PathwayProgressDecision decision = progressor.advance(new PathwayProgressCommand(
            graph(), "ASSESS", PathwayAdvanceEventType.COMPLETE, "SURGERY"));

        assertThat(decision.nextNodeCode()).isEqualTo("SURGERY");
        assertThat(decision.edgeType()).isEqualTo(PathwayEdgeType.CONDITION);
    }

    @Test
    void completesPathwayWhenCurrentNodeHasNoOutgoingEdge() {
        PathwayProgressDecision decision = progressor.advance(new PathwayProgressCommand(
            graph(), "FOLLOWUP", PathwayAdvanceEventType.COMPLETE, null));

        assertThat(decision.nextNodeCode()).isNull();
        assertThat(decision.status()).isEqualTo(PatientPathwayStatus.COMPLETED);
    }

    @Test
    void varianceCanContinueToRequestedReachableNode() {
        PathwayProgressDecision decision = progressor.advance(new PathwayProgressCommand(
            graph(), "LAB", PathwayAdvanceEventType.VARIANCE, "FOLLOWUP"));

        assertThat(decision.nextNodeCode()).isEqualTo("FOLLOWUP");
        assertThat(decision.status()).isEqualTo(PatientPathwayStatus.NODE_EXECUTING);
    }

    @Test
    void varianceWithoutContinuationStaysAtCurrentNode() {
        PathwayProgressDecision decision = progressor.advance(new PathwayProgressCommand(
            graph(), "LAB", PathwayAdvanceEventType.VARIANCE, null));

        assertThat(decision.nextNodeCode()).isEqualTo("LAB");
        assertThat(decision.status()).isEqualTo(PatientPathwayStatus.VARIANCE);
    }

    @Test
    void rejectsTargetNodeOutsideCurrentOutgoingEdges() {
        assertThatThrownBy(() -> progressor.advance(new PathwayProgressCommand(
            graph(), "ASSESS", PathwayAdvanceEventType.COMPLETE, "UNKNOWN")))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_PATHWAY_006);
    }

    private PathwayGraph graph() {
        String tenantId = "tenant-A";
        String templateId = "pt-" + tenantId;
        return new PathwayGraph(
            List.of(
                node("ASSESS", 10, false),
                node("LAB", 20, false),
                node("SURGERY", 30, false),
                node("FOLLOWUP", 40, true)
            ),
            List.of(
                edge("e1", tenantId, templateId, "ASSESS", "LAB", PathwayEdgeType.DEFAULT, 10),
                edge("e2", tenantId, templateId, "ASSESS", "SURGERY", PathwayEdgeType.CONDITION, 20),
                edge("e3", tenantId, templateId, "LAB", "FOLLOWUP", PathwayEdgeType.DEFAULT, 10),
                edge("e4", tenantId, templateId, "SURGERY", "FOLLOWUP", PathwayEdgeType.DEFAULT, 10)
            )
        );
    }

    private PathwayNode node(String code, int sortOrder, boolean terminal) {
        Instant now = Instant.now();
        return new PathwayNode(
            null, "pn-" + code, "tenant-A", "pt-tenant-A", code, code,
            PathwayNodeType.ASSESSMENT, sortOrder, "医生", null, 120, terminal,
            null, now, "tester", now, "tester", "trace-pathway");
    }

    private PathwayEdge edge(String edgeId, String tenantId, String templateId,
                             String from, String to, PathwayEdgeType type, int priority) {
        Instant now = Instant.now();
        return new PathwayEdge(
            null, edgeId, tenantId, templateId, edgeId, from, to, type,
            null, priority, now, "tester", now, "tester", "trace-pathway");
    }
}
