package me.mykindos.betterpvp.champions.settings.menus;

import me.mykindos.betterpvp.core.client.Client;
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

public class ChampionsSettingsMenu extends AbstractGui implements SettingCategory {

    public ChampionsSettingsMenu(Client client) {
        super(9, 1);

        final Description cooldownSoundsDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.COOLDOWN_SOUNDS_ENABLED).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.JUKEBOX)
                    .displayName(Component.text("Sounds", color))
                    .lore(Component.text("Whether to play a sound when a cooldown has expired or not", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();


        addItems(new SettingsButton(client, ClientProperty.COOLDOWN_SOUNDS_ENABLED, cooldownSoundsDescription));
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
                        .material(Material.NETHERITE_SWORD)
                        .displayName(Component.text("Champions Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View generic settings related to champions", NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
