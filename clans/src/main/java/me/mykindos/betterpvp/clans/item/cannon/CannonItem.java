package me.mykindos.betterpvp.clans.item.cannon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.item.cannon.ability.CannonPlaceAbility;
import me.mykindos.betterpvp.clans.item.cannon.model.CannonManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = false)
public class CannonItem extends BaseItem implements ReloadHook {

    private final CannonPlaceAbility cannonPlaceAbility;

    @Inject
    private CannonItem(Clans clans, CannonManager cannonManager, CannonPlaceAbility cannonPlaceAbility) {
        super("Cannon", Item.model("cannon", 64), ItemGroup.WEAPON, ItemRarity.RARE);
        this.cannonPlaceAbility = cannonPlaceAbility;
        this.cannonPlaceAbility.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(cannonPlaceAbility).build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Clans.class, this);
        double cooldown = config.getConfig("cooldown", 30.0, Double.class);
        cannonPlaceAbility.setCooldown(cooldown);
    }
}
