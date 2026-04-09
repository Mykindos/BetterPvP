package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;

import java.util.Map;
import java.util.UUID;

public class PlayerQueuePolicyResolver {

    private final OrchestrationServiceConfig config;

    public PlayerQueuePolicyResolver(OrchestrationServiceConfig config) {
        this.config = config;
    }

    public ResolvedQueuePolicy resolve(UUID playerUuid, Map<UUID, PlayerRankSnapshot> rankByPlayer) {
        final PlayerRankSnapshot snapshot = rankByPlayer.get(playerUuid);
        final String rankName = snapshot == null ? config.defaultRank() : config.normalizeRankName(snapshot.rank());
        final OrchestrationServiceConfig.RankPolicy policy = config.policyForRank(rankName);
        return new ResolvedQueuePolicy(rankName, policy.priority(), policy.bypass());
    }

    public record ResolvedQueuePolicy(String rankName, int basePriority, boolean bypass) {
    }
}
