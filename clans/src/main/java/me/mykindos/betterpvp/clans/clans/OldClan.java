package me.mykindos.betterpvp.clans.clans;

import lombok.CustomLog;
import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IOldClan;

import java.util.UUID;

@CustomLog
@Data
public class OldClan implements IOldClan {
    private final UUID id;
    private String name;
}
