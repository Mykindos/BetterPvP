package me.mykindos.betterpvp.clans.settings.menus;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
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

public class ClansSettingsMenu extends AbstractGui implements SettingCategory {

    public ClansSettingsMenu(Client client) {
        super(9, 1);

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


        addItems(new SettingsButton(client, ClientProperty.SIDEBAR_ENABLED, sidebarDescription));

        final Description mapLocationsDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.MAP_POINTS_OF_INTEREST).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.MAP)
                    .displayName(Component.text("Map Points of Interest", color))
                    .lore(Component.text("Whether to show points of interest on the map or not", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();


        addItems(new SettingsButton(client, ClientProperty.MAP_POINTS_OF_INTEREST, mapLocationsDescription));

        final Description mapPlayerNamesDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.MAP_PLAYER_NAMES).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.FILLED_MAP)
                    .displayName(Component.text("Map Player Names", color))
                    .lore(Component.text("Whether or not to display player names on the map", NamedTextColor.GRAY))
                    .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                    .frameLore(true)
                    .build()
                    .get();
        }).build();


        addItems(new SettingsButton(client, ClientProperty.MAP_PLAYER_NAMES, mapPlayerNamesDescription));

        final Description territoryPopup = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.TERRITORY_POPUPS_ENABLED).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Component.text("Territory Popups", color))
                    .lore(Component.text("Whether or not to display territory popups", NamedTextColor.GRAY))
                    .flag(ItemFlag.HIDE_ITEM_SPECIFICS)
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        addItems(new SettingsButton(client, ClientProperty.TERRITORY_POPUPS_ENABLED, territoryPopup));
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
                        .material(Material.IRON_DOOR)
                        .displayName(Component.text("Clans Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View generic settings related to the clans gamemode", NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
