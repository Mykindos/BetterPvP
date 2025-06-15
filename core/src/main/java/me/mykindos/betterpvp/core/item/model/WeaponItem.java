package me.mykindos.betterpvp.core.item.model;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.repo.MeleeDamageStat;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.inventory.ItemStack;

public abstract class WeaponItem extends BaseItem implements ReloadHook {

    private final BPvPPlugin plugin;

    protected WeaponItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity) {
        super(name, model, ItemGroup.WEAPON, rarity);
        this.plugin = plugin;
        addSerializableComponent(new StatContainerComponent().withBaseStat(new MeleeDamageStat(1f)));
        addSerializableComponent(new RuneContainerComponent(2));
    }

    @Override
    public void reload() {
        final ItemConfig itemConfig = ItemConfig.of(plugin, this);
        final double damage = itemConfig.getConfig("damage", 1.0, Double.class);
        getComponent(StatContainerComponent.class).ifPresent(statContainer -> {
            statContainer.getStat(MeleeDamageStat.class).ifPresent(meleeDamage -> {
                statContainer.withBaseStat(meleeDamage.withValue(damage));
            });
        });
    }
}
