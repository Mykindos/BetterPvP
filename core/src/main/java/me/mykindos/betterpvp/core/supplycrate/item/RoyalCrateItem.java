package me.mykindos.betterpvp.core.supplycrate.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateController;
import me.mykindos.betterpvp.core.supplycrate.impl.RoyalCrateType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

@Singleton
@PluginAdapter("ModelEngine")
@ItemKey("core:royal_supply_crate")
public class RoyalCrateItem extends BaseItem {

    @Inject
    private RoyalCrateItem(SupplyCrateController controller, RoyalCrateType type) {
        super("Royal Crate Deployment", Item.model("supply_crate", 1), ItemGroup.MISC, ItemRarity.LEGENDARY);
        final DeployCrateAbility ability = new DeployCrateAbility(controller, type, "the center of the map", this::getLocation, true);
        ability.setConsumesItem(true);
        addBaseComponent(AbilityContainerComponent.builder().ability(ability).build());
    }

    private Location getLocation(Client client) {
        final World world = Objects.requireNonNull(client.getGamer().getPlayer()).getWorld();
        return new Location(world, 0, world.getMaxHeight() - 5, 0);
    }
}
