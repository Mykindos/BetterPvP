package me.mykindos.betterpvp.core.world.zone;

import lombok.Builder;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable bundle of context passed to {@link ZoneRule#evaluate(ZoneActionContext)}. Carries who is acting, the zone
 * they are acting in, what kind of action it is, and the optional block/location it concerns.
 */
@Value
@Builder
public class ZoneActionContext {

    Player player;
    Zone zone;
    ZoneInteraction interaction;
    @Nullable Location location;
    @Nullable Block block;
}
