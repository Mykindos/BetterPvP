package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.text.NumberFormat;

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
        final TextComponent currentEnergy = Component.text("Current Energy: ", NamedTextColor.GRAY)
                .append(Component.text(NumberFormat.getInstance().format(clan.getEnergy()), NamedTextColor.YELLOW));

        final TextColor highlight = TextColor.color(227, 156, 255);
        final ItemView energy = ItemView.builder()
                .material(Material.PAPER)
                .customModelData(4)
                .displayName(Component.text("Energy", TextColor.color(179, 79, 255), TextDecoration.BOLD))
                .frameLore(true)
                .lore(Component.text("Energy is required to upkeep your", NamedTextColor.GRAY))
                .lore(Component.text("clan core and territory. Without it", NamedTextColor.GRAY))
                .lore(Component.text("your clan will disband.", NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(currentEnergy)
                .lore(Component.text("Disbands in: ", NamedTextColor.GRAY).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.YELLOW)))
                .lore(Component.empty())
                .lore(Component.text("To get more energy, you can:", NamedTextColor.GRAY))
                .lore(Component.text("\u25AA ").append(Component.text("Kill other players", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Complete dungeons and raids", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Mine it in the world or at Fields", highlight)))
                .lore(Component.text("\u25AA ").append(Component.text("Participate in world events", highlight)))
                .build();
        setItem(12, new SimpleItem(energy));

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
