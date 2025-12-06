package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;

/**
 * A data class used to track each player's current state and update the animation for each player.
 */
@Data
@RequiredArgsConstructor
public class RisingUppercutData {

    /**
     * The start time in milliseconds that this ability was activated by the player.
     */
    private @NotNull Long startTime;

    /**
     * The duration of the slashing animation in milliseconds.
     */
    private final long animationDuration;

    /**
     * The item display that is updated every tick and does the "slashing."
     */
    private final @NotNull ItemDisplay itemDisplay;

    /**
     * The initial location of the caster (player using the slash).
     */
    private final @NotNull Location castingLocation;
}
