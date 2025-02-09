package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
@Getter
public class WorldLogHandler extends Manager<WorldLogSession> {

    private final WorldLogRepository worldLogRepository;

    private final Set<UUID> inspectingPlayers = new java.util.HashSet<>();
    private final List<WorldLog> pendingLogs = new ArrayList<>();

    @Inject
    public WorldLogHandler(WorldLogRepository worldLogRepository) {
        this.worldLogRepository = worldLogRepository;
    }

    public void saveLogs(List<WorldLog> logs) {
        worldLogRepository.saveLogs(logs);
    }

    public WorldLogSession getSession(UUID uuid) {
        return objects.computeIfAbsent(uuid.toString(), k -> new WorldLogSession());
    }
}
