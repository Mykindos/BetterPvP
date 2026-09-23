package me.mykindos.betterpvp.core.world.settler.crew;

import lombok.Value;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** How big and how fast one job's crew can get. */
@Value
public class CrewLimits {

    public static final CrewLimits NONE = new CrewLimits(Integer.MAX_VALUE, Double.MAX_VALUE, Map.of(), 0);

    int maxSize;
    /** The most times faster than its listed time a job can run. */
    double maxSpeed;
    /** The most of each rarity on one crew. A rarity left out has no limit of its own. */
    @NotNull Map<SettlerRarity, Integer> perRarity;
    /** Extra speed, as a share, for a Builder whose compatible trade is already on the crew. */
    double compatibleBonus;
}
