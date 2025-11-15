package me.mykindos.betterpvp.core.client.achievements.repository;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.NamespacedKey;

import java.time.LocalDateTime;

@Data
public class AchievementCompletion {
    private final long id;
    private final Client client;
    private final NamespacedKey key;
    private final String period;
    private final LocalDateTime timestamp;
    private int completedRank;
    private int totalCompletions;
}
