package com.medkernel.engine.pathway;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

@Component
public class PathwayProgressor {

    public PathwayProgressDecision advance(PathwayProgressCommand command) {
        if (command == null || command.graph() == null || isBlank(command.currentNodeCode())
                || command.eventType() == null) {
            throw new ApiException(ErrorCode.ENG_PATHWAY_006, "路径推进命令不完整");
        }
        PathwayNode current = findCurrentNode(command.graph(), command.currentNodeCode());
        if (command.eventType() == PathwayAdvanceEventType.EXIT) {
            return new PathwayProgressDecision(current.nodeCode(), null, PatientPathwayStatus.EXITED, null);
        }
        if (command.eventType() == PathwayAdvanceEventType.VARIANCE && isBlank(command.requestedNextNodeCode())) {
            return new PathwayProgressDecision(
                current.nodeCode(), current.nodeCode(), PatientPathwayStatus.VARIANCE, null);
        }

        List<PathwayEdge> outgoing = command.graph().edges().stream()
            .filter(edge -> Objects.equals(edge.fromNodeCode(), current.nodeCode()))
            .sorted(Comparator
                .comparing((PathwayEdge edge) -> edge.priority() == null ? 0 : edge.priority())
                .thenComparing(PathwayEdge::edgeId))
            .toList();
        if (outgoing.isEmpty() || Boolean.TRUE.equals(current.terminalFlag())) {
            return new PathwayProgressDecision(current.nodeCode(), null, PatientPathwayStatus.COMPLETED, null);
        }

        PathwayEdge selected = isBlank(command.requestedNextNodeCode())
            ? outgoing.getFirst()
            : outgoing.stream()
                .filter(edge -> Objects.equals(edge.toNodeCode(), command.requestedNextNodeCode()))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                    ErrorCode.ENG_PATHWAY_006,
                    "目标节点不属于当前节点的可达出边: " + command.requestedNextNodeCode()));
        ensureTargetNodeExists(command.graph(), selected.toNodeCode());
        return new PathwayProgressDecision(
            current.nodeCode(), selected.toNodeCode(), PatientPathwayStatus.NODE_EXECUTING, selected.edgeType());
    }

    private PathwayNode findCurrentNode(PathwayGraph graph, String currentNodeCode) {
        return graph.nodes().stream()
            .filter(node -> Objects.equals(node.nodeCode(), currentNodeCode))
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PATHWAY_006, "当前节点不存在: " + currentNodeCode));
    }

    private void ensureTargetNodeExists(PathwayGraph graph, String nodeCode) {
        boolean exists = graph.nodes().stream().anyMatch(node -> Objects.equals(node.nodeCode(), nodeCode));
        if (!exists) {
            throw new ApiException(ErrorCode.ENG_PATHWAY_006, "目标节点不存在: " + nodeCode);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
