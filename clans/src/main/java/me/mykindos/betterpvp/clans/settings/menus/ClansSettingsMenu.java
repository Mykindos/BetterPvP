package me.mykindos.betterpvp.clans.settings.menus;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarMode;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.settings.menus.buttons.EnumSettingsButton;
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

    public ClansSettingsMenu(Client client, Windowed previous) {
        super(9, 2);

        final Description sidebarDescription = Description.builder().icon(lang -> {
            final SidebarMode mode = SidebarMode.parse(client.getProperty(ClientProperty.SIDEBAR_MODE).orElse(null));
            final NamedTextColor color = mode == SidebarMode.DISABLED ? NamedTextColor.RED : NamedTextColor.GREEN;
            return ItemView.builder()
                    .material(Material.IRON_BARS)
                    .displayName(Translations.component("clans.menu.settings.button.sidebar.name").color(color))
                    .lore(Translations.component("clans.menu.settings.button.sidebar.lore").color(NamedTextColor.GRAY))
                    .lore(Translations.component("clans.menu.settings.button.sidebar.mode",
                            Component.text(mode.name(), color)).color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        fill(0, 9, Menu.BACKGROUND_GUI_ITEM, false);
        setItem(4, new BackButton(previous));

        addItems(new EnumSettingsButton(client, ClientProperty.SIDEBAR_MODE, SidebarMode.HUD, sidebarDescription));

        final Description mapLocationsDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.MAP_POINTS_OF_INTEREST).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.MAP)
                    .displayName(Translations.component("clans.menu.settings.button.map-poi.name").color(color))
                    .lore(Translations.component("clans.menu.settings.button.map-poi.lore").color(NamedTextColor.GRAY))
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
                    .displayName(Translations.component("clans.menu.settings.button.map-player-names.name").color(color))
                    .lore(Translations.component("clans.menu.settings.button.map-player-names.lore").color(NamedTextColor.GRAY))
                    .hideAdditionalTooltip(true)
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
                    .displayName(Translations.component("clans.menu.settings.button.territory-popups.name").color(color))
                    .lore(Translations.component("clans.menu.settings.button.territory-popups.lore").color(NamedTextColor.GRAY))
                    .hideAdditionalTooltip(true)
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        addItems(new SettingsButton(client, ClientProperty.TERRITORY_POPUPS_ENABLED, territoryPopup));
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Translations.component("clans.menu.settings.title");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.IRON_DOOR)
                        .displayName(Translations.component("clans.menu.settings.tab.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                        .lore(Translations.component("clans.menu.settings.tab.lore").color(NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
