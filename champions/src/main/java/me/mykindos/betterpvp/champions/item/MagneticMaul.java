package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.MagnetismAbility;
import me.mykindos.betterpvp.champions.item.ability.ReverseKnockbackAbility;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class MagneticMaul extends WeaponItem implements ReloadHook {

    private static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Material.MUSIC_DISC_FAR.key())
            .customModelData(1)
            .build()
            .get();

    private final MagnetismAbility magnetismAbility;
    private final ReverseKnockbackAbility reverseKnockbackAbility;

    @Inject
    private MagneticMaul(Champions champions,
                         MagnetismAbility magnetismAbility,
                         ItemFactory itemFactory) {
        super(champions, "Magnetic Maul", model, ItemRarity.LEGENDARY);
        this.magnetismAbility = magnetismAbility;
        this.reverseKnockbackAbility = new ReverseKnockbackAbility(champions, itemFactory, this);
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(magnetismAbility)
                .ability(reverseKnockbackAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final ItemConfig config = ItemConfig.of(Champions.class, this);
        
        // Configure Magnetism ability
        magnetismAbility.setPullRange(config.getConfig("pullRange", 10.0, Double.class));
        magnetismAbility.setPullFov(config.getConfig("pullFov", 80.3, Double.class));
        magnetismAbility.setEnergyPerTick(config.getConfig("energyPerTick", 2.0, Double.class));
        
        // Configure Reverse Knockback ability
        reverseKnockbackAbility.setKnockbackMultiplier(config.getConfig("knockbackMultiplier", -1.0, Double.class));
        reverseKnockbackAbility.setBypassMinimum(config.getConfig("bypassMinimumKnockback", true, Boolean.class));
    }
} 