package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.EffectRouletteAbility;
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
public class SuspiciousStew extends BaseItem implements ReloadHook {

    private final EffectRouletteAbility effectRouletteAbility;

    @Inject
    private SuspiciousStew(EffectRouletteAbility effectRouletteAbility) {
        super("Suspicious Stew", ItemStack.of(Material.SUSPICIOUS_STEW), ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        this.effectRouletteAbility = effectRouletteAbility;
        this.effectRouletteAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(effectRouletteAbility).build());
    }

    @Override
    public void reload() {
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        double duration = config.getConfig("duration", 5.0, Double.class);
        double cooldown = config.getConfig("cooldown", 8.0, Double.class);
        effectRouletteAbility.setDuration(duration);
        effectRouletteAbility.setCooldown(cooldown);
    }
}
