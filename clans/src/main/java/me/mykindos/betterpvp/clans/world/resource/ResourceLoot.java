package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Rolls and awards a resource node's named loot table via the core loot system. This is the seam that lets every
 * Fields drop type (ores, energy shards, gold chunks, loot chests) become data: a {@code lootTable} id resolved at
 * harvest time instead of a hardcoded {@code FieldsInteractable} subclass.
 */
@Singleton
public class ResourceLoot {

    private final LootTableRegistry lootTableRegistry;

    @Inject
    public ResourceLoot(@NotNull LootTableRegistry lootTableRegistry) {
        this.lootTableRegistry = lootTableRegistry;
    }

    /**
     * Rolls {@code lootTableId} for {@code player} and awards the result at {@code location}. Unknown table ids
     * fail-soft to an empty table (logged once by the registry).
     */
    public void award(@NotNull String lootTableId, @NotNull Player player, @NotNull Location location) {
        final LootTable table = lootTableRegistry.loadLootTable(lootTableId);
        final LootContext context = new LootContext(
                LootSession.newSession(table, player),
                location,
                LootSource.of("Resource Node", "resource:" + lootTableId));
        table.generateLoot(context).award();
    }

    /**
     * Where a harvested block's loot should land: on the face the player is actually looking at, pulled slightly back
     * towards them, rather than at the block's centre. A block mined from above otherwise drops its loot inside the
     * block below it, and one mined from the side drops it out of reach behind the face.
     * <p>
     * Falls back to the block's centre when the ray trace finds nothing, which happens when the block has already been
     * changed out from under the trace.
     */
    public static @NotNull Location dropLocation(@NotNull Player player, @NotNull Block block) {
        final AttributeInstance reach = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE));
        final RayTraceResult result = player.rayTraceBlocks(reach.getValue());
        if (result == null) {
            return block.getLocation().toCenterLocation();
        }
        final Location hit = result.getHitPosition().toLocation(block.getWorld());
        return hit.subtract(player.getLocation().getDirection().multiply(0.5));
    }
}
