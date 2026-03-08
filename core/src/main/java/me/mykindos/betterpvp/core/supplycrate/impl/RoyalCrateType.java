package me.mykindos.betterpvp.core.supplycrate.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateType;

@Singleton
@PluginAdapter("ModelEngine")
public class RoyalCrateType implements SupplyCrateType {

    private final LootTableRegistry lootTableRegistry;

    @Inject
    private RoyalCrateType(LootTableRegistry lootTableRegistry) {
        this.lootTableRegistry = lootTableRegistry;
    }

    @Override
    public double getSize() {
        return 2.0;
    }

    @Override
    public double getFallSpeed() {
        return 2.5f;
    }

    @Override
    public String getDisplayName() {
        return "Royal Crate";
    }

    @Override
    public String getModelId() {
        return "red_supply_crate";
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public LootTable getLootTable() {
        return this.lootTableRegistry.loadLootTable("royal_crate");
    }
}
