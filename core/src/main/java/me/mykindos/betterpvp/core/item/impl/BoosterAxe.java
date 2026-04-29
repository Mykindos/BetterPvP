package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;

@Singleton
@ItemKey("core:booster_axe")
@FallbackItem(value = Material.GOLDEN_AXE, keepRecipes = true)
public class BoosterAxe extends VanillaItem implements Reloadable {

    private static final int DEFAULT_DURABILITY = 32;

    public BoosterAxe() {
        super("Booster Axe", Material.GOLDEN_AXE, ItemRarity.UNCOMMON);
        addSerializableComponent(new DurabilityComponent(DEFAULT_DURABILITY));
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(config.getConfig("durability", DEFAULT_DURABILITY, Integer.class));
        });
    }
}
