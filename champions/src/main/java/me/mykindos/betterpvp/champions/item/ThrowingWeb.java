package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.item.ability.ThrowingWebAbility;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:throwing_web")
@EqualsAndHashCode(callSuper = false)
public class ThrowingWeb extends BaseItem implements Reloadable {

    private final ThrowingWebAbility throwingWebAbility;

    @Inject
    private ThrowingWeb(ChampionsManager championsManager, WorldBlockHandler blockHandler, CooldownManager cooldownManager) {
        super("Throwing Web", ItemStack.of(Material.COBWEB), ItemGroup.WEAPON, ItemRarity.UNCOMMON);
        this.throwingWebAbility = new ThrowingWebAbility(championsManager, blockHandler, cooldownManager);
        throwingWebAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(throwingWebAbility).build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double duration = config.getConfig("duration", 2.5, Double.class);
        double throwableExpiry = config.getConfig("throwableExpiry", 10.0, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        throwingWebAbility.setDuration(duration);
        throwingWebAbility.setThrowableExpiry(throwableExpiry);
        throwingWebAbility.setCooldown(cooldown);
    }
}
