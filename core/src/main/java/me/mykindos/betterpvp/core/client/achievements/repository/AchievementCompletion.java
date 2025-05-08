package me.mykindos.betterpvp.core.client.achievements.repository;

import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import org.bukkit.NamespacedKey;

@Data
public class AchievementCompletion implements Unique {
    private final UUID id;
    private final UUID user;
    private final NamespacedKey key;
    private final Timestamp timestamp;

    @Override
    public UUID getUniqueId() {
        return id;
    }
}
