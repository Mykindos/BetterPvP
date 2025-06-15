package me.mykindos.betterpvp.clans.item.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.item.cannon.ability.CannonballReloadAbility;
import me.mykindos.betterpvp.clans.item.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class CannonballItem extends BaseItem {

    private static final ItemStack model = ItemView.builder().material(Material.SHULKER_SHELL).customModelData(2).build().get();

    @Inject
    private CannonballItem(Clans clans, CannonManager cannonManager, CannonballReloadAbility cannonballReloadAbility) {
        super("Cannonball", model, ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        cannonballReloadAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(cannonballReloadAbility).build());
    }

} 