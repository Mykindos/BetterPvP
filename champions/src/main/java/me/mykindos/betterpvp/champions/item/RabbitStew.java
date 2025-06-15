package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.SpeedBoostAbility;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class RabbitStew extends BaseItem implements ReloadHook {

    private final SpeedBoostAbility speedBoostAbility;

    @Inject
    private RabbitStew(SpeedBoostAbility speedBoostAbility) {
        super("Rabbit Stew", ItemStack.of(Material.RABBIT_STEW), ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.speedBoostAbility = speedBoostAbility;
        this.speedBoostAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(speedBoostAbility).build());
    }

    @Override
    public void reload() {
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        double duration = config.getConfig("duration", 7.0, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        int level = config.getConfig("level", 1, Integer.class);
        speedBoostAbility.setDuration(duration);
        speedBoostAbility.setCooldown(cooldown);
        speedBoostAbility.setLevel(level);
    }
}
