package me.mykindos.betterpvp.core.item.impl.buildersbox;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;

@Singleton
@ItemKey("core:builders_box")
@PluginAdapter("ModelEngine")
public class BuildersBox extends BaseItem {

    private final LootTableRegistry lootTableRegistry;

    @Inject
    private BuildersBox(LootTableRegistry lootTableRegistry) {
        super("Builder's Box", Item.model("builders_box", 1), ItemGroup.MISC, ItemRarity.LEGENDARY);
        this.lootTableRegistry = lootTableRegistry;
        final BuildersBoxAbility ability = new BuildersBoxAbility(this::getLootTable, "Builder's Box");
        ability.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, ability)
                .build());
    }

    private LootTable getLootTable() {
        return lootTableRegistry.loadLootTable("builders_box");
    }
}
