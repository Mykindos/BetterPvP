package me.mykindos.betterpvp.core.item.impl;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@ItemKey("core:standard_pickaxe")
@FallbackItem(value = Material.IRON_PICKAXE, keepRecipes = true)
public class StandardPickaxe extends BaseItem implements Reloadable {

    private static final int DEFAULT_DURABILITY = 250;

    public StandardPickaxe() {
        super("Standard Pickaxe", ItemStack.of(Material.IRON_PICKAXE), ItemGroup.TOOL, ItemRarity.COMMON);
        addSerializableComponent(new DurabilityComponent(DEFAULT_DURABILITY));
        addSerializableComponent(new RuneContainerComponent());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(config.getConfig("durability", DEFAULT_DURABILITY, Integer.class));
        });
    }
}
