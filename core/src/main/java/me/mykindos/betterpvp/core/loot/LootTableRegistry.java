package me.mykindos.betterpvp.core.loot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.loot.loader.LootTableLoader;
import me.mykindos.betterpvp.core.loot.loader.SupabaseLootTableLoader;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing loot tables.
 */
@PluginAdapter("Core")
@Singleton
@CustomLog
public class LootTableRegistry implements ReloadHook {

    /**
     * The map of registered loot tables.
     */
    private final Map<String, LootTable> lootTables = new HashMap<>();
    private final LootTableLoader loader;

    @Inject
    private LootTableRegistry(SupabaseLootTableLoader supabaseLoader) {
        this.loader = supabaseLoader;
    }

    /**
     * Gets the map of registered loot tables.
     * @return The map of registered loot tables.
     */
    public Map<String, LootTable> getLoaded() {
        return Collections.unmodifiableMap(this.lootTables);
    }

    /**
     * Gets a loot table by its ID.
     * @param id The ID of the loot table to retrieve.
     * @return The loot table with the given ID, or an empty loot table if not found.
     */
    public LootTable loadLootTable(String id) {
        if (!this.lootTables.containsKey(id)) {
            log.error("Tried to load loot table {} but it was not found in the loaded tables, using an empty loot table instead!", id).submit();
            return LootTable.builder().id(id).build();
        }
        return this.lootTables.get(id);
    }

    /**
     * Reloads the configuration or state of this class.
     */
    @Override
    public void reload() {
        ((SupabaseLootTableLoader) this.loader).reloadCredentials();
        this.lootTables.clear();
        for (LootTable lootTable : loader.load()) {
            this.lootTables.put(lootTable.getId(), lootTable);
        }
    }
}
