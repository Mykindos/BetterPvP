package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.EnergyButton;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoreMenu extends AbstractGui implements Windowed {

    private final Clan clan;
    private final Player player;

    public CoreMenu(Clan clan, Player player) {
        super(9, 3);
        this.clan = clan;
        this.player = player;
        populate();
    }

    private void populate() {
        setItem(12, new EnergyButton(clan, true, null));

        final TextColor highlight2 = TextColor.color(115, 140, 255);
        final ItemView.ItemViewBuilder vaultItem = ItemView.builder()
                .material(Material.ENDER_CHEST)
                .displayName(Component.text("Clan Vault", TextColor.color(84, 115, 255), TextDecoration.BOLD))
                .frameLore(true)
                .lore(Component.text("The clan vault is a shared storage", NamedTextColor.GRAY))
                .lore(Component.text("for your clan members. It is a safe", NamedTextColor.GRAY))
                .lore(Component.text("place to store your items.", NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Component.text("Only clan members ranked Admin and", NamedTextColor.GRAY))
                .lore(Component.text("above can access the vault.", NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Component.text("Your vault has limited slots. To", NamedTextColor.GRAY))
                .lore(Component.text("gain more slots, you can:", NamedTextColor.GRAY))
                .lore(Component.text("\u25AA ").append(Component.text("Level up your clan", highlight2)))
                .lore(Component.text("\u25AA ").append(Component.text("Purchase more slots in the shop", highlight2)));

        final ClanVault vault = clan.getCore().getVault();
        if (vault.hasPermission(player)) {
            vaultItem.action(ClickActions.ALL, Component.text("Open"));
        } else {
            vaultItem.lore(Component.empty())
                    .lore(Component.text("You cannot access the Clan Vault", TextColor.color(255, 71, 93), TextDecoration.BOLD));
        }

        setItem(14, new SimpleItem(vaultItem.build(), click -> {
            final Player viewer = click.getPlayer();
            if (!vault.hasPermission(viewer)) {
                UtilMessage.message(viewer, "Clans", "You do not have permission to access the clan vault.");
                return;
            }

            if (vault.isLocked()) {
                UtilMessage.message(viewer, "Clans", "<red>The clan vault is currently in use by: <dark_red>%s</dark_red>.", vault.getLockedBy());
                return;
            }

            vault.show(viewer);
            new SoundEffect(Sound.BLOCK_CHEST_OPEN, 0.8F, 0.7F).play(viewer.getLocation());
        }));

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Clan Core");
    }
}
