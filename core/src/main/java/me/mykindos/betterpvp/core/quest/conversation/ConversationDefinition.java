package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A published conversation: dialogue nodes, each owning its own responses. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationDefinition {

    private String id;
    private String name;
    private String startNodeId;
    private List<ConvNode> nodes = new ArrayList<>();

    public Optional<ConvNode> node(String nodeId) {
        return nodes.stream().filter(n -> n.getId().equals(nodeId)).findFirst();
    }

    public Optional<ConvNode> startNode() {
        if (startNodeId != null) {
            Optional<ConvNode> explicit = node(startNodeId);
            if (explicit.isPresent()) return explicit;
        }
        // Fall back to a node nothing branches into, else the first node.
        return nodes.stream()
                .filter(n -> nodes.stream().noneMatch(other -> branchesTo(other, n.getId())))
                .findFirst()
                .or(() -> nodes.stream().findFirst());
    }

    /** Responses leaving a node, in authored order. */
    public List<ConvResponse> responses(String nodeId) {
        return node(nodeId).map(n -> n.getData().getResponses()).orElse(List.of());
    }

    /** Whether any response on {@code from} branches (goto) to {@code targetId}. */
    private static boolean branchesTo(ConvNode from, String targetId) {
        return from.getData().getResponses().stream().anyMatch(r -> {
            ConvOutcome o = r.getOutcome();
            return o != null && "goto".equals(o.getKind()) && targetId.equals(o.getTarget());
        });
    }
}
