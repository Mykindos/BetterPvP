package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.item.ability.EnergyBoost;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class EnergyApple extends BaseItem implements ReloadHook {

    private final EnergyBoost energyBoost;

    @Inject
    private EnergyApple(EnergyHandler energyHandler, CooldownManager cooldownManager) {
        super("Energy Apple", ItemStack.of(Material.APPLE), ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        final SoundEffect soundEffect = new SoundEffect(Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        this.energyBoost = new EnergyBoost(energyHandler, cooldownManager, soundEffect);
        energyBoost.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(energyBoost).build());
    }

    @Override
    public void reload() {
        final ItemConfig config = ItemConfig.of(Core.class, this);
        double energy = config.getConfig("energy", 0.25, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        energyBoost.setEnergy(energy);
        energyBoost.setCooldown(cooldown);
    }
}
