package me.mykindos.betterpvp.progression.settings;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
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

public class ProfessionSettingsMenu extends AbstractGui implements SettingCategory {

    public ProfessionSettingsMenu(Client client, Windowed previous) {
        super(9, 2);

        final Description disableTreefellerDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.DISABLE_TREEFELLER).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.GOLDEN_AXE)
                    .displayName(Component.text("Disable Treefeller", color))
                    .lore(Component.text("Disables treefeller from activating", NamedTextColor.GRAY))
                    .hideAdditionalTooltip(true)
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        fill(0, 9, Menu.BACKGROUND_GUI_ITEM, false);
        setItem(4, new BackButton(previous));

        addItems(new SettingsButton(client, ClientProperty.DISABLE_TREEFELLER, disableTreefellerDescription));

    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Profession Settings");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_PICKAXE)
                        .displayName(Component.text("Profession Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View settings related to Professions", NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .hideAdditionalTooltip(true)
                        .frameLore(true)
                        .build())
                .build();
    }
}
