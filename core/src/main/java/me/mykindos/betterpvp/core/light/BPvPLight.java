package me.mykindos.betterpvp.core.light;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class BPvPLight {
    private final UUID id;
    private final UUID playerID;
    private final String source;
    private final int level;

    public BPvPLight(UUID playerID, String source, int level) {
        this.level = level;
        this.id = UUID.randomUUID();
        this.playerID = playerID;
        this.source = source;
    }
}
