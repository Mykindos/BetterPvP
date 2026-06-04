package me.mykindos.betterpvp.clans.world.resource.archetype;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.resource.ResourceArchetype;
import me.mykindos.betterpvp.clans.world.resource.ResourceLoot;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fishing archetype: turns a pond into a loot node. When a player reels in a catch whose hook sat inside the node's
 * zone, the vanilla fish reward is suppressed and the node's named loot table is rolled instead, so a pond can drop
 * anything the loot system supports rather than vanilla fish.
 * <p>
 * The manager has already filtered to {@link PlayerFishEvent.State#CAUGHT_FISH} inside this node and run the
 * profession gate, so {@link #onFish} does no zone or state checking — it only replaces the reward.
 * <p>
 * <b>Lava ponds:</b> the {@code fishing:} config section carries a {@code liquid} key (default {@code "water"}). A
 * {@code lava} pond reuses this exact loot-replacement flow with a different loot table; nothing here branches on the
 * liquid because the replacement is identical. Vanilla bobbers cannot physically settle in lava, so the casting
 * mechanic that would let a hook reach a {@code CAUGHT_FISH} state over lava is a separate concern and is deliberately
 * not implemented in this class.
 */
@Singleton
@CustomLog
public class FishingArchetype implements ResourceArchetype {

    private final ResourceLoot loot;

    @Inject
    public FishingArchetype(@NotNull ResourceLoot loot) {
        this.loot = loot;
    }

    @Override
    public @NotNull String id() {
        return "fishing";
    }

    @Override
    public boolean onFish(@NotNull ResourceNodeProp node, @NotNull Player player, @NotNull PlayerFishEvent event) {
        final String lootTable = node.getDefinition().getLootTable();
        if (lootTable == null) {
            return false; // no table configured — let the vanilla catch proceed
        }

        // Remove the vanilla fish entity so the node's loot table replaces (rather than stacks on top of) the reward.
        if (event.getCaught() instanceof Entity caught) {
            caught.remove();
        }

        loot.award(lootTable, player, player.getLocation());
        return true;
    }
}
