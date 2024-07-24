package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ViewClanButton extends SimpleItem {

    private final Clan viewerClan;
    private final Clan clan;

    public ViewClanButton(@NotNull Clan viewerClan, @NotNull Clan clan) {
        this(viewerClan, clan, new ArrayList<>());
    }

    public ViewClanButton(@NotNull Clan viewerClan, @NotNull Clan clan, @NotNull List<Component> extraLore) {
        super(ItemView.of(clan.getBanner().get()).toBuilder()
                .displayName(Component.text(clan.getName(), NamedTextColor.GREEN))
                .lore(new ArrayList<>(extraLore))
                .lore(UtilMessage.deserialize("<gray><white>%,d</white>/<white>%,d</white> Online", clan.getOnlineMemberCount(), clan.getMembers().size()))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("View Clan"))
                .build());
        this.viewerClan = viewerClan;
        this.clan = clan;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new ClanMenu(player, viewerClan, clan).show(player);
    }

}