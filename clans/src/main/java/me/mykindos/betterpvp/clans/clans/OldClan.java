package me.mykindos.betterpvp.clans.clans;

import lombok.CustomLog;
import lombok.Data;

import java.util.UUID;

@CustomLog
@Data
public class OldClan {
    private final UUID id;
    private String name;
}
