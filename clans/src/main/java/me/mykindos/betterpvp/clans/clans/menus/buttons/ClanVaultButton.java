package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
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

public class ClanVaultButton extends ControlItem<ClanMenu> {

    private final Clan clan;

    public ClanVaultButton(Clan clan) {
        this.clan = clan;
    }

    @Override
    public ItemProvider getItemProvider(ClanMenu clanMenu) {
        return ItemView.builder()
                .material(Material.ENDER_CHEST)
                .displayName(Component.text("Clan Vault", NamedTextColor.GOLD, TextDecoration.BOLD))
                .action(ClickActions.ALL, Component.text("Open"))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.chat("/clan vault");
    }
}
