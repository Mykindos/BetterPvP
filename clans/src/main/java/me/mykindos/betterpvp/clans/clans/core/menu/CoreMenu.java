package me.mykindos.betterpvp.clans.clans.core.menu;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.core.mailbox.ClanMailbox;
import me.mykindos.betterpvp.clans.clans.core.mailbox.ClanMailboxButton;
import me.mykindos.betterpvp.clans.clans.core.vault.ClanVault;
import me.mykindos.betterpvp.clans.clans.menus.buttons.EnergyButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.locale.Translations;
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
    private final ItemFactory itemFactory;

    public CoreMenu(Clan clan, Player player, ItemFactory itemFactory) {
        super(9, 3);
        this.clan = clan;
        this.player = player;
        this.itemFactory = itemFactory;
        populate();
    }

    private void populate() {
        setItem(11, new EnergyButton(clan, true, null));

        final TextColor highlight2 = TextColor.color(115, 140, 255);
        final ItemView.ItemViewBuilder vaultItem = ItemView.builder()
                .material(Material.ENDER_CHEST)
                .displayName(Translations.component("clans.menu.core.button.vault.name").color(TextColor.color(84, 115, 255)).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.1").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.2").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.3").color(NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.4").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.5").color(NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.6").color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.menu.core.button.vault.lore.description.7").color(NamedTextColor.GRAY))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.core.button.vault.lore.method.level").color(highlight2)))
                .lore(Component.text("\u25AA ").append(Translations.component("clans.menu.core.button.vault.lore.description.8").color(highlight2)));

        final ClanVault vault = clan.getCore().getVault();
        if (vault.hasPermission(player)) {
            vaultItem.action(ClickActions.ALL, Translations.component("clans.menu.core.button.vault.action"));
        } else {
            vaultItem.lore(Component.empty())
                    .lore(Translations.component("clans.menu.core.button.vault.lore.no-permission").color(TextColor.color(255, 71, 93)).decorate(TextDecoration.BOLD));
        }

        setItem(13, new SimpleItem(vaultItem.build(), click -> {
            final Player viewer = click.getPlayer();
            if (!vault.hasPermission(viewer)) {
                UtilMessage.message(viewer, "clans.prefix", "clans.core.vault.no-permission");
                return;
            }

            if (vault.isLocked()) {
                UtilMessage.message(viewer, "clans.prefix", "clans.core.vault.locked", Component.text(vault.getLockedBy(), NamedTextColor.DARK_RED));
                return;
            }

            vault.show(viewer, this);
            new SoundEffect(Sound.BLOCK_CHEST_OPEN, 0.8F, 0.7F).play(viewer.getLocation());
        }));

        final ClanMailbox mailbox = clan.getCore().getMailbox();
        setItem(15, new ClanMailboxButton(mailbox, itemFactory, this));


        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.menu.core.title");
    }
}
