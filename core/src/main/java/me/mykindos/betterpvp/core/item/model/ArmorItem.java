package me.mykindos.betterpvp.core.item.model;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.inventory.ItemStack;

public abstract class ArmorItem extends BaseItem implements Reloadable {

    protected final BPvPPlugin plugin;

    protected ArmorItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity) {
        super(name, model, ItemGroup.ARMOR, rarity);
        this.plugin = plugin;
        addSerializableComponent(new StatContainerComponent().withBaseStat(new ItemStat<>(StatTypes.HEALTH, 1d)));
        addSerializableComponent(new DurabilityComponent(500));
        addSerializableComponent(new RuneContainerComponent(0, 0));
    }

    @Override
    public void reload() {
        final Config config = Config.item(plugin, this);
        final double health = config.getConfig("health.base", 1, Double.class);
        final double healthMin = config.getConfig("health.min", 0, Double.class);
        final double healthMax = config.getConfig("health.max", 2, Double.class);
        getComponent(StatContainerComponent.class).ifPresent(statContainer -> {
            statContainer.getStat(StatTypes.HEALTH).ifPresent(healthStat -> {
                statContainer.withBaseStat(healthStat.withValue(health).withRanges(healthMin, healthMax));
            });
        });

        final int durability = config.getConfig("durability", 500, Integer.class);
        getComponent(DurabilityComponent.class).ifPresent(durabilityComponent -> {
            durabilityComponent.setMaxDamage(durability);
        });
    }
}
