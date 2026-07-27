package me.mykindos.betterpvp.core.item.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.impl.cannon.ability.CannonballReloadAbility;

/**
 * A cannonball that bursts into bomblets instead of landing one heavy hit. Shares the reload ability with every other
 * round - which round it loads is resolved from the item key by
 * {@link me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmoRegistry}.
 */
@Singleton
@ItemKey("core:cluster_cannonball")
@EqualsAndHashCode(callSuper = false)
public class ClusterCannonballItem extends BaseItem {

    @Inject
    private ClusterCannonballItem(Core core, CannonballReloadAbility cannonballReloadAbility) {
        super(translatableName("core.item.cluster_cannonball.name"), Item.model("cannonball", 16),
                ItemGroup.MATERIAL, ItemRarity.RARE);
        cannonballReloadAbility.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, cannonballReloadAbility)
                .build());
    }
}
