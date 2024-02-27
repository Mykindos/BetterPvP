package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
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
        final ClanVault vault = clan.getVault();
        if (vault.isLocked()) {
            UtilMessage.message(player, "Clans", "<red>The clan vault is currently in use by: <dark_red>%s</dark_red>.", vault.getLockedBy());
            return;
        }

        vault.show(player, getGui());
    }
}
