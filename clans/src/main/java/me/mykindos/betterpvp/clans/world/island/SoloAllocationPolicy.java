package me.mykindos.betterpvp.clans.world.island;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The current occupancy rule: every player gets their own instance, so an existing instance is never offered to a
 * second player.
 */
public class SoloAllocationPolicy implements InstanceAllocationPolicy {

    @Override
    public boolean canAccept(@NotNull IslandInstance instance, @NotNull Player player) {
        return false;
    }

    @Override
    public int capacity() {
        return 1;
    }

}
