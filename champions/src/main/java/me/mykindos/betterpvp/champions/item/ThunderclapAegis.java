package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.VolticBashAbility;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class ThunderclapAegis extends WeaponItem implements ReloadHook {

    private static final ItemStack model;

    private final VolticBashAbility volticBashAbility;
    private final ItemFactory itemFactory;

    static {
        model = ItemView.builder()
                .material(Material.SHIELD)
                .customModelData(99)
                // Make the shield unbreakable directly on the model
                .flag(ItemFlag.HIDE_UNBREAKABLE)
                .build()
                .get();

        // Set unbreakable flag on the item meta
        model.editMeta(meta -> meta.setUnbreakable(true));
    }

    @Inject
    private ThunderclapAegis(Champions champions,
                            VolticBashAbility volticBashAbility,
                            ItemFactory itemFactory) {
        super(champions, "Thunderclap Aegis", model, ItemRarity.LEGENDARY);
        this.volticBashAbility = volticBashAbility;
        this.itemFactory = itemFactory;
        
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(volticBashAbility)
                .build());
    }
    
    @Override
    public void reload() {
        super.reload();
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        
        // Configure VolticBash ability
        volticBashAbility.setVelocity(config.getConfig("velocity", 0.8, Double.class));
        volticBashAbility.setMaxChargeTicks(config.getConfig("maxChargeTicks", 60, Integer.class));
        volticBashAbility.setEnergyOnCollide(config.getConfig("energyOnCollide", 25.0, Double.class));
        volticBashAbility.setChargeDamage(config.getConfig("chargeDamage", 7.0, Double.class));
        volticBashAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.0, Double.class));
    }
} 