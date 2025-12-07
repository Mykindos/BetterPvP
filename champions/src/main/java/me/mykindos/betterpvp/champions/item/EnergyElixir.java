package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.EnergyBoost;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:energy_elixir")
public class EnergyElixir extends BaseItem implements Reloadable {

    private final EnergyBoost energyBoost;

    @Inject
    private EnergyElixir(EnergyService energyService, CooldownManager cooldownManager) {
        super("Energy Elixir", ItemStack.of(Material.HONEY_BOTTLE), ItemGroup.CONSUMABLE, ItemRarity.UNCOMMON);
        final SoundEffect soundEffect = new SoundEffect(Sound.ITEM_HONEY_BOTTLE_DRINK, 1.2f, 1f);
        this.energyBoost = new EnergyBoost(energyService, cooldownManager, soundEffect);
        energyBoost.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(energyBoost).build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double energyRegen = config.getConfig("energyRegen", 0.50, Double.class);
        double cooldown = config.getConfig("cooldown", 12.0, Double.class);
        energyBoost.setEnergy(energyRegen);
        energyBoost.setCooldown(cooldown);
    }
}

