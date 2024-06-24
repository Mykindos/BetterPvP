package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

public class ShopTransportButton extends ControlItem<ClanTravelHubMenu> {

    private final Clan clan;
    private final Material material;
    private final NamedTextColor namedTextColor;

    public ShopTransportButton(Clan clan, Material material, NamedTextColor namedTextColor) {
        this.clan = clan;
        this.material = material;
        this.namedTextColor = namedTextColor;
    }

    @Override
    public ItemProvider getItemProvider(ClanTravelHubMenu clanTravelHubMenu) {
        ItemView.ItemViewBuilder provider = ItemView.builder().material(material)
                .displayName(Component.text(clan.getName(), namedTextColor, TextDecoration.BOLD))
                .action(ClickActions.LEFT, Component.text("Teleport"));
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if(clickType.isLeftClick()) {
            if(clan.getHome() != null) {
                player.teleport(clan.getHome());
            }
        }
    }
}
