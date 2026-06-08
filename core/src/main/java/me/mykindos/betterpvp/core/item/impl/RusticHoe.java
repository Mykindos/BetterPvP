package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:rustic_hoe")
@FallbackItem(value = Material.WOODEN_HOE, keepRecipes = true)
public class RusticHoe extends BaseItem implements Reloadable {

    private static final int DEFAULT_DURABILITY = 59;

    public RusticHoe() {
        super(translatableName("core.item.rustic-hoe.name"), ItemStack.of(Material.WOODEN_HOE), ItemGroup.TOOL, ItemRarity.COMMON);
        addSerializableComponent(new DurabilityComponent(DEFAULT_DURABILITY));
        addSerializableComponent(new RepairableComponent());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(config.getConfig("durability", DEFAULT_DURABILITY, Integer.class));
        });
    }
}
