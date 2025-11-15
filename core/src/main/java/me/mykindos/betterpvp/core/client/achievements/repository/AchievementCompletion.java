package me.mykindos.betterpvp.core.client.achievements.repository;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import org.bukkit.NamespacedKey;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AchievementCompletion implements Unique {
    private final UUID id;
    private final Client client;
    private final NamespacedKey key;
    private final String period;
    private final LocalDateTime timestamp;
    private int completedRank;
    private int totalCompletions;

    @Override
    public UUID getUniqueId() {
        return id;
    }
}
