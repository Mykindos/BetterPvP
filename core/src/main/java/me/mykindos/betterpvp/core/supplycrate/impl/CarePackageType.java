package me.mykindos.betterpvp.core.supplycrate.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateType;

@Singleton
@PluginAdapter("ModelEngine")
public class CarePackageType implements SupplyCrateType {

    private final LootTableRegistry lootTableRegistry;

    @Inject
    private CarePackageType(LootTableRegistry lootTableRegistry) {
        this.lootTableRegistry = lootTableRegistry;
    }

    @Override
    public String getDisplayName() {
        return "Care Package";
    }

    @Override
    public String getModelId() {
        return "green_supply_crate";
    }

    @Override
    public LootTable getLootTable() {
        return this.lootTableRegistry.loadLootTable("care_package");
    }

}
