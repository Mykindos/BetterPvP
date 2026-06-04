package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
}
