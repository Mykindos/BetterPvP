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
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:ancient_pickaxe")
@FallbackItem(value = Material.NETHERITE_PICKAXE, keepRecipes = true)
public class AncientPickaxe extends BaseItem implements Reloadable {

    private static final int DEFAULT_DURABILITY = 2031;

    public AncientPickaxe() {
        super("Ancient Pickaxe", ItemStack.of(Material.NETHERITE_PICKAXE), ItemGroup.TOOL, ItemRarity.RARE);
        addSerializableComponent(new DurabilityComponent(DEFAULT_DURABILITY));
        addSerializableComponent(new RepairableComponent());
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
