package me.mykindos.betterpvp.clans.world.voyage;

import lombok.Value;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/** One place a crew can be set ashore in a world, under the name its marker carries. */
@Value
public class ArrivalPoint {

    @NotNull String name;

    @NotNull Location location;
}
