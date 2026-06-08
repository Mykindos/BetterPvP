package me.mykindos.betterpvp.champions.settings;

import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
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

public class ChampionsSettingsMenu extends AbstractGui implements SettingCategory {

    public ChampionsSettingsMenu(Client client, Windowed previous) {
        super(9, 2);

        final Description skillTooltipDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ChampionsProperty.SKILL_WEAPON_TOOLTIP).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WOODEN_SWORD)
                    .displayName(Translations.component("champions.menu.settings.tooltips.name").color(color))
                    .lore(Translations.component("champions.menu.settings.tooltips.lore").color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        fill(0, 9, Menu.BACKGROUND_GUI_ITEM, false);
        setItem(4, new BackButton(previous));
        addItems(new SettingsButton(client, ChampionsProperty.SKILL_WEAPON_TOOLTIP, skillTooltipDescription));


    }

    @NotNull
    @Override
    public Component getTitle() {
        return Translations.component("champions.menu.settings.title");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_CHESTPLATE)
                        .displayName(Translations.component("champions.menu.settings.button.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                        .lore(Translations.component("champions.menu.settings.button.lore").color(NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
