package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.VolticBashAbility;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
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
        model = Item.model("thunderclap_aegis", 1);
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.BLOCK)
                .build());

        // Set unbreakable flag on the item meta
        model.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
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
        final Config config = Config.item(Champions.class, this);
        
        // Configure VolticBash ability
        volticBashAbility.setVelocity(config.getConfig("velocity", 0.8, Double.class));
        volticBashAbility.setMaxChargeTicks(config.getConfig("maxChargeTicks", 60, Integer.class));
        volticBashAbility.setEnergyOnCollide(config.getConfig("energyOnCollide", 25.0, Double.class));
        volticBashAbility.setChargeDamage(config.getConfig("chargeDamage", 7.0, Double.class));
        volticBashAbility.setEnergyPerTick(config.getConfig("energyPerTick", 1.0, Double.class));
    }
} 