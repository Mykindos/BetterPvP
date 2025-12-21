package me.mykindos.betterpvp.core.client.achievements.repository;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@Data
public class AchievementCompletion {
    private final long id;
    private final Client client;
    private final NamespacedKey key;
    private final StatFilterType achievementFilterType;
    @Nullable
    private final Object period;
    private final LocalDateTime timestamp;
    private int completedRank;
    private int totalCompletions;
}
