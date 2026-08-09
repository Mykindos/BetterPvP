package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A player as the cursor pass sees them: position, facing and clan, captured once per tick on the main thread.
 * <p>
 * Viewers read these instead of querying every other player themselves, which keeps the cursor layer to one capture
 * pass per tick plus cheap arithmetic per viewer, rather than work proportional to viewers times players.
 */
@Value
public class PlayerMark {

    UUID uuid;
    String name;
    int x;
    int z;
    String world;
    /** Facing as a map cursor direction, 0–15 clockwise from north. */
    byte direction;
    @Nullable Long clanId;
}
