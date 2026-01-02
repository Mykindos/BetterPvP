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

import java.util.Collections;
import java.util.List;

public abstract class WeaponItem extends BaseItem implements Reloadable {

    private final BPvPPlugin plugin;
    private final List<Group> groups;

    protected WeaponItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity, List<Group> groups) {
        super(name, model, ItemGroup.WEAPON, rarity);
        this.plugin = plugin;
        this.groups = groups;
        addSerializableComponent(new RuneContainerComponent(2));
        addSerializableComponent(new DurabilityComponent(500));

        if (groups.contains(Group.MELEE)) {
            addSerializableComponent(new StatContainerComponent()
                    .withBaseStat(new ItemStat<>(StatTypes.MELEE_DAMAGE, 1.0))
                    .withBaseStat(new ItemStat<>(StatTypes.MELEE_ATTACK_SPEED, 0.0)));
        }
    }

    protected WeaponItem(BPvPPlugin plugin, String name, ItemStack model, ItemRarity rarity) {
        this(plugin, name, model, rarity, List.of(Group.MELEE));
    }

    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    @Override
    public void reload() {
        final Config config = Config.item(plugin, this);
        final double damage = config.getConfig("damage.base", 1.0, Double.class);
        final double damageMin = config.getConfig("damage.min", 0.0, Double.class);
        final double damageMax = config.getConfig("damage.max", 2.0, Double.class);
        getComponent(StatContainerComponent.class).ifPresent(statContainer -> {
            statContainer.getStat(StatTypes.MELEE_DAMAGE).ifPresent(meleeDamage -> {
                statContainer.withBaseStat(meleeDamage.withValue(damage).withRanges(damageMin, damageMax));
            });
        });

        final double attackSpeed = config.getConfig("attack_speed.base", 0.0, Double.class);
        final double attackSpeedMin = config.getConfig("attack_speed.min", -0.25, Double.class);
        final double attackSpeedMax = config.getConfig("attack_speed.max", 0.25, Double.class);
        getComponent(StatContainerComponent.class).ifPresent(statContainer -> {
            statContainer.getStat(StatTypes.MELEE_ATTACK_SPEED).ifPresent(meleeAttackSpeed -> {
                statContainer.withBaseStat(meleeAttackSpeed.withValue(attackSpeed).withRanges(attackSpeedMin, attackSpeedMax));
            });
        });

        final int durability = config.getConfig("durability", 500, Integer.class);
        getComponent(DurabilityComponent.class).ifPresent(durabilityComponent -> {
            durabilityComponent.setMaxDamage(durability);
        });
    }

    public enum Group {
        RANGED,
        MELEE
    }
}
