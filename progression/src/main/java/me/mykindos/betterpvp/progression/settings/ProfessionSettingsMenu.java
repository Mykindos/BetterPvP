package me.mykindos.betterpvp.progression.settings;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class ProfessionSettingsMenu extends AbstractGui implements SettingCategory {

    public ProfessionSettingsMenu(Client client, Windowed previous) {
        super(9, 2);

        fill(0, 9, Menu.BACKGROUND_GUI_ITEM, false);
        setItem(4, new BackButton(previous));
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Translations.component("progression.menu.settings.title");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_PICKAXE)
                        .displayName(Translations.component("progression.menu.settings.button.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                        .lore(Translations.component("progression.menu.settings.button.lore").color(NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .hideAdditionalTooltip(true)
                        .frameLore(true)
                        .build())
                .build();
    }
}
