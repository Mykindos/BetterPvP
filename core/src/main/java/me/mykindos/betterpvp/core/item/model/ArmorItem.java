package me.mykindos.betterpvp.core.item.model;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.HealthStat;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.inventory.ItemStack;

public abstract class ArmorItem extends BaseItem implements Reloadable {

    protected final BPvPPlugin plugin;

    protected ArmorItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity) {
        super(name, model, ItemGroup.ARMOR, rarity);
        this.plugin = plugin;
        addSerializableComponent(new StatContainerComponent().withBaseStat(new HealthStat(1)));
        addSerializableComponent(new DurabilityComponent(500));
        addSerializableComponent(new RuneContainerComponent(2));
    }

    @Override
    public void reload() {
        final Config config = Config.item(plugin, this);
        final int health = config.getConfig("health", 1, Integer.class);
        getComponent(StatContainerComponent.class).ifPresent(statContainer -> {
            statContainer.getStat(HealthStat.class).ifPresent(healthStat -> {
                statContainer.withBaseStat(healthStat.withValue(health));
            });
        });

        final int durability = config.getConfig("durability", 500, Integer.class);
        getComponent(DurabilityComponent.class).ifPresent(durabilityComponent -> {
            durabilityComponent.setMaxDamage(durability);
        });
    }
}
