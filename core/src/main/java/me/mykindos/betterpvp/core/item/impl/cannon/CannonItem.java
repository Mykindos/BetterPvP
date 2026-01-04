package me.mykindos.betterpvp.core.item.impl.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.cannon.ability.CannonPlaceAbility;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

@Singleton
@ItemKey("core:cannon")
@PluginAdapter("ModelEngine")
@EqualsAndHashCode(callSuper = false)
public class CannonItem extends BaseItem implements Reloadable {

    private final CannonPlaceAbility cannonPlaceAbility;

    @Inject
    private CannonItem(Core core, CannonPlaceAbility cannonPlaceAbility) {
        super("Cannon", Item.model("cannon", 64), ItemGroup.WEAPON, ItemRarity.RARE);
        this.cannonPlaceAbility = cannonPlaceAbility;
        this.cannonPlaceAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(cannonPlaceAbility).build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, this);
        double cooldown = config.getConfig("cooldown", 30.0, Double.class);
        cannonPlaceAbility.setCooldown(cooldown);
    }
}
