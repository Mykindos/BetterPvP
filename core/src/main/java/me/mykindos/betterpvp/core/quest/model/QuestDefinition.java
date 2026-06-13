package me.mykindos.betterpvp.core.quest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A published quest, deserialized from {@code content.published}. Mirrors the
 * admin console's quest schema: metadata + requirements + rewards + a stage
 * graph (nodes/edges).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestDefinition {

    private String id;
    private String name;
    private String questType = "side";
    private String scope = "solo";
    /** When false (default), a completed quest cannot be started again. */
    private boolean repeatable = false;
    private List<PrimitiveData> requirements = new ArrayList<>();
    private List<PrimitiveData> rewards = new ArrayList<>();
    private List<QuestNode> nodes = new ArrayList<>();
    private List<QuestEdge> edges = new ArrayList<>();

    public Optional<QuestNode> node(String nodeId) {
        return nodes.stream().filter(n -> n.getId().equals(nodeId)).findFirst();
    }

    /** Root stages = nodes with no incoming edge. These start when the quest begins. */
    public List<QuestNode> rootStages() {
        return nodes.stream()
                .filter(n -> edges.stream().noneMatch(e -> e.getTarget().equals(n.getId())))
                .toList();
    }

    /** Stages reachable directly from the given stage. */
    public List<QuestNode> nextStages(String stageId) {
        return edges.stream()
                .filter(e -> e.getSource().equals(stageId))
                .map(e -> node(e.getTarget()).orElse(null))
                .filter(n -> n != null)
                .toList();
    }

    public Optional<QuestEdge> edge(String source, String target) {
        return edges.stream().filter(e -> e.getSource().equals(source) && e.getTarget().equals(target)).findFirst();
    }
}
