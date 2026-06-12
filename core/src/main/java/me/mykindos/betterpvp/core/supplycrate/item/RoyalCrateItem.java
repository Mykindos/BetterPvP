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
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import me.mykindos.betterpvp.core.supplycrate.SupplyCrateController;
import me.mykindos.betterpvp.core.supplycrate.impl.RoyalCrateType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

@Singleton
@PluginAdapter("ModelEngine")
@ItemKey("core:royal_supply_crate")
public class RoyalCrateItem extends BaseItem {

    @Inject
    private RoyalCrateItem(SupplyCrateController controller, RoyalCrateType type) {
        super(translatableName("core.item.royal-supply-crate.name"), Item.model("supply_crate", 1), ItemGroup.MISC, ItemRarity.LEGENDARY);
        final DeployCrateAbility ability = new DeployCrateAbility(controller, type, "the center of the map", this::getLocation, true);
        ability.setConsumesItem(true);
        addBaseComponent(DescriptionComponent.translatable(1, "core.item.royal-supply-crate.lore",
                Component.text(3, NamedTextColor.YELLOW),
                Component.text(10, NamedTextColor.YELLOW)));
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, ability)
                .build());
    }

    private Location getLocation(Client client) {
        final World world = Objects.requireNonNull(client.getGamer().getPlayer()).getWorld();
        return new Location(world, -25, world.getMaxHeight() - 5, 20);
    }
}
