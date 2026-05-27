package com.medkernel.engine.pathway;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

/**
 * 路径确定性推进器。
 *
 * <p>根据路径图、当前节点、推进事件和可选目标节点选择下一节点或终态，只做流程判断，
 * 不读取数据库、不写审计、不生成医疗诊断或医嘱。
 */
@Component
public class PathwayProgressor {

    /**
     * 计算一次路径推进决策。
     *
     * <p>规则：退出事件直接进入 {@code EXITED}；无继续节点的变异停留在当前节点并进入
     * {@code VARIANCE}；普通完成事件优先使用请求目标节点，否则选择优先级最高的出边。
     *
     * @throws ApiException 当命令不完整、当前节点不存在或目标节点不可达时抛出 {@code ENG-PATHWAY-006}
     */
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
        if (!isBlank(command.requestedNextNodeCode()) && outgoing.isEmpty()) {
            throw new ApiException(
                ErrorCode.ENG_PATHWAY_006,
                "当前节点没有可达出边，不能指定目标节点: " + command.requestedNextNodeCode());
        }
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
