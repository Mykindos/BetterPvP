package me.mykindos.betterpvp.core.quest;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime state of an in-progress quest for a single participant scope (a player
 * for solo quests, a clan for clan quests, etc.). Objective progress is keyed by
 * {@code stageId/objectiveId}.
 */
@Data
public class QuestInstance {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ABANDONED = "abandoned";

    private final UUID instanceId;
    private final String questId;
    private final String scopeType;
    private final String scopeId;
    private String status = STATUS_ACTIVE;
    private final Set<String> currentStages = new HashSet<>();
    private final Map<String, Integer> progress = new HashMap<>();
    private final Map<String, Integer> targets = new HashMap<>();

    public static String objectiveKey(String stageId, String objectiveId) {
        return stageId + "/" + objectiveId;
    }

    public boolean isStageComplete(String stageId) {
        return progress.entrySet().stream()
                .filter(e -> e.getKey().startsWith(stageId + "/"))
                .allMatch(e -> e.getValue() >= targets.getOrDefault(e.getKey(), 1));
    }
}
