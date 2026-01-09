package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.item.ability.EchoPebbleAbility;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
@ItemKey("champions:echo_pebble")
@EqualsAndHashCode(callSuper = false)
public class EchoPebble extends BaseItem implements Reloadable {

    private final @NotNull EchoPebbleAbility echoPebbleAbility;

    @Inject
    private EchoPebble(Champions champions, ChampionsManager championsManager, CooldownManager cooldownManager) {
        super("Echo Pebble", Item.model("echo_pebble", 64), ItemGroup.WEAPON, ItemRarity.UNCOMMON);
        this.echoPebbleAbility = new EchoPebbleAbility(champions, championsManager, cooldownManager);
        echoPebbleAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(echoPebbleAbility).build());
    }

    @Override
    public void reload() {
        final @NotNull Config config = Config.item(Champions.class, this);

        final double velocity = config.getConfig("velocity", 1.5, Double.class);
        final double revealDuration = config.getConfig("revealDuration", 5.0, Double.class);
        final double radius = config.getConfig("radius", 5.0, Double.class);
        final boolean needsLineOfSight = config.getConfig("needsLineOfSight", false, Boolean.class);
        final double throwableExpiry = config.getConfig("throwableExpiry", 10.0, Double.class);
        final double cooldown = config.getConfig("cooldown", 10.0, Double.class);

        echoPebbleAbility.setVelocity(velocity);
        echoPebbleAbility.setRevealDuration(revealDuration);
        echoPebbleAbility.setRadius(radius);
        echoPebbleAbility.setNeedsLineOfSight(needsLineOfSight);
        echoPebbleAbility.setThrowableExpiry(throwableExpiry);
        echoPebbleAbility.setCooldown(cooldown);
    }
}
