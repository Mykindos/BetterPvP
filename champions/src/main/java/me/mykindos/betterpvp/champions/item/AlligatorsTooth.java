package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.GatorStrokeAbility;
import me.mykindos.betterpvp.champions.item.ability.UnderwaterBreathingAbility;
import me.mykindos.betterpvp.champions.item.ability.WaterDamageAbility;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class AlligatorsTooth extends WeaponItem implements ReloadHook {

    private final GatorStrokeAbility gatorStrokeAbility;
    private final WaterDamageAbility waterDamageAbility;
    private final UnderwaterBreathingAbility underwaterBreathingAbility;
    private final ItemFactory itemFactory;

    @Inject
    private AlligatorsTooth(Champions champions, 
                           GatorStrokeAbility gatorStrokeAbility,
                           UnderwaterBreathingAbility underwaterBreathingAbility,
                           EnergyHandler energyHandler,
                           ItemFactory itemFactory) {
        super(champions, "Alligator's Tooth", Item.model("alligators_tooth"), ItemRarity.LEGENDARY);
        this.gatorStrokeAbility = gatorStrokeAbility;
        this.underwaterBreathingAbility = underwaterBreathingAbility;
        this.waterDamageAbility = new WaterDamageAbility(champions, itemFactory, this);
        this.itemFactory = itemFactory;
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(gatorStrokeAbility)
                .ability(underwaterBreathingAbility)
                .ability(waterDamageAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure GatorStroke ability
        gatorStrokeAbility.setVelocityStrength(config.getConfig("velocityStrength", 0.7, Double.class));
        gatorStrokeAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.0, Double.class));
        gatorStrokeAbility.setSkimmingEnergyMultiplier(config.getConfig("skimmingEnergyMultiplier", 3.0, Double.class));
        
        // Configure damage values
        double bonusDamage = config.getConfig("bonusDamage", 4.0, Double.class);
        waterDamageAbility.setBonusDamage(bonusDamage);
    }
} 