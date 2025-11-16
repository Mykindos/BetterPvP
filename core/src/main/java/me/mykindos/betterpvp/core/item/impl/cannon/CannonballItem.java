package me.mykindos.betterpvp.core.item.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.impl.cannon.ability.CannonballReloadAbility;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonManager;

@Singleton
@ItemKey("core:cannonball")
@EqualsAndHashCode(callSuper = false)
public class CannonballItem extends BaseItem {

    @Inject
    private CannonballItem(Core core, CannonManager cannonManager, CannonballReloadAbility cannonballReloadAbility) {
        super("Cannonball", Item.model("cannonball", 16), ItemGroup.MATERIAL, ItemRarity.UNCOMMON);
        cannonballReloadAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(cannonballReloadAbility).build());
    }

} 