package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class WorldLogHandler {

    private final WorldLogRepository worldLogRepository;

    @Getter
    private final List<WorldLog> pendingLogs = new ArrayList<>();

    @Inject
    public WorldLogHandler(WorldLogRepository worldLogRepository) {
        this.worldLogRepository = worldLogRepository;
    }

    public void saveLogs(List<WorldLog> logs) {
        worldLogRepository.saveLogs(logs);
    }
}
