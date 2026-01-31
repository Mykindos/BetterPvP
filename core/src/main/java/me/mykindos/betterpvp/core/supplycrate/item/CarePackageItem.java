package me.mykindos.betterpvp.core.supplycrate.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateController;
import me.mykindos.betterpvp.core.supplycrate.impl.CarePackageType;
import org.bukkit.Location;

import java.util.Objects;

@Singleton
@PluginAdapter("ModelEngine")
@ItemKey("core:care_package")
public class CarePackageItem extends BaseItem {

    @Inject
    private CarePackageItem(SupplyCrateController controller, CarePackageType type) {
        super("Care Package Deployment", Item.model("supply_crate", 1), ItemGroup.MISC, ItemRarity.EPIC);
        final DeployCrateAbility ability = new DeployCrateAbility(controller, type, "your current location", this::getLocation, false);
        ability.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, ability)
                .build());
    }

    private Location getLocation(Client client) {
        final Location location = Objects.requireNonNull(client.getGamer().getPlayer()).getLocation();
        location.setY(location.getWorld().getMaxHeight() - 5);
        return location;
    }
}
