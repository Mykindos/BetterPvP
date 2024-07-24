package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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

public class HomeTransportButton extends ControlItem<ClanTravelHubMenu> {

    private final Clan clan;


    public HomeTransportButton(Clan clan) {
        this.clan = clan;
    }

    @Override
    public ItemProvider getItemProvider(ClanTravelHubMenu clanTravelHubMenu) {
        ItemView.ItemViewBuilder provider = ItemView.builder().material(Material.RED_BED)
                .displayName(Component.text("Clan Home - ", NamedTextColor.GOLD).append(Component.text(clan.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)))
                .action(ClickActions.LEFT, Component.text("Teleport"));
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (clickType.isLeftClick()) {
            if (clan.getCore().isSet()) {
                clan.getCore().teleport(player, true);
            } else {
                UtilMessage.simpleMessage(player, "Clans", "Your clan does not have a core set!");
            }
        }
    }
}
