package me.mykindos.betterpvp.progression.profession.fishing.bait;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.BaitAbility;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all bait items.
 * Manages configuration and ability integration.
 */
public abstract class BaitItem extends BaseItem implements ReloadHook {

    private final BPvPPlugin plugin;
    private final BaitAbility ability;
    
    /**
     * Creates a new bait item
     *
     * @param name    The name of the item
     * @param model   The item model
     * @param rarity  The item rarity
     * @param ability The bait ability
     */
    protected BaitItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity, BaitAbility ability) {
        super(name, model, ItemGroup.TOOL, rarity);
        this.ability = ability;
        this.plugin = plugin;
        
        // Add the ability to the item
        addBaseComponent(AbilityContainerComponent.builder().ability(ability).build());
    }
    
    @Override
    public void reload() {
        // Load configuration for the bait ability
        final ItemConfig config = ItemConfig.of(plugin, this);
        ability.setRadius(config.getConfig("radius", 5.0, Double.class));
        ability.setMultiplier(config.getConfig("multiplier", 1.0, Double.class));
        ability.setDuration(config.getConfig("duration", 180L, Long.class));
    }
} 