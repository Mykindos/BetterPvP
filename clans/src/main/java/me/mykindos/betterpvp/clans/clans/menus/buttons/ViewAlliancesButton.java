package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ViewAlliancesButton extends ViewClanCollectionButton {

    private final Clan clan;
    private final Clan viewerClan;

    public ViewAlliancesButton(Clan clan, Windowed parent, Clan viewerClan) {
        super(ItemView.builder().material(Material.PAPER).customModelData(2).build(),
                "Alliances", parent);
        this.clan = clan;
        this.viewerClan = viewerClan;
    }

    @Override
    protected Collection<Clan> getPool() {
        return clan.getAlliances().stream().map(ClanAlliance::getClan).map(Clan.class::cast).toList();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        List<Item> alliances = clan.getAlliances().stream().map(alliance -> {
            final Clan alliedClan = (Clan) alliance.getClan();
            final TextColor color = alliance.isTrusted() ? NamedTextColor.GREEN : NamedTextColor.RED;
            final List<Component> lore = List.of(Component.text("Trusted: ", NamedTextColor.GRAY)
                    .append(Component.text(alliance.isTrusted(), color)));
            return (Item) new ViewClanButton(viewerClan, alliedClan, lore);
        }).toList();

        new ViewCollectionMenu(collectionName, alliances, parent).show(player);
    }
}
