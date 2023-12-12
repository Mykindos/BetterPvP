package me.mykindos.betterpvp.clans.settings.menus;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;

public class ClansSettingsMenu extends AbstractGui implements SettingCategory {

    public ClansSettingsMenu(Client client) {
        super(9, 1);
        final Gamer gamer = client.getGamer();

        final Description sidebarDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.SIDEBAR_ENABLED).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.IRON_BARS)
                    .displayName(Component.text("Sidebar", color))
                    .lore(Component.text("Whether to display the sidebar or not", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description clanMenuDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) gamer.getProperty(GamerProperty.CLAN_MENU_ENABLED).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.BAMBOO_HANGING_SIGN)
                    .displayName(Component.text("Clan Menu", color))
                    .lore(Component.text("Whether to display a menu or chat message when doing /clan", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        addItems(
                new SettingsButton(client, ClientProperty.SIDEBAR_ENABLED, sidebarDescription),
                new SettingsButton(gamer, GamerProperty.CLAN_MENU_ENABLED, clanMenuDescription)
        );
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Clans Settings");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_HELMET)
                        .displayName(Component.text("Clans Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View generic settings related to the clans gamemode", NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
