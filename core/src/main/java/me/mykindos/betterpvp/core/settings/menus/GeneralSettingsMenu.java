package me.mykindos.betterpvp.core.settings.menus;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class GeneralSettingsMenu extends AbstractGui implements SettingCategory {

    public GeneralSettingsMenu(final Player player, final Client client) {
        super(9, 1);
        final Gamer gamer = client.getGamer();

        final Description tipDescription = Description.builder().icon(lang -> {
            final boolean tipSetting = (boolean) client.getProperty(ClientProperty.TIPS_ENABLED).orElse(false);
            final NamedTextColor color = tipSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Component.text("Tips", color))
                    .lore(Component.text("Whether to enable tips or not.", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description chatDescription = Description.builder().icon(lang -> {
            final boolean chatSetting = (boolean) client.getProperty(ClientProperty.CHAT_ENABLED).orElse(false);
            final NamedTextColor color = chatSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Component.text("Chat", color))
                    .lore(Component.text("Whether to enable chat or not.", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description dropDescription = Description.builder().icon(lang -> {
            final boolean chatSetting = (boolean) client.getProperty(ClientProperty.DROP_PROTECTION_ENABLED).orElse(false);
            final NamedTextColor color = chatSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Component.text("Drop Protection", color))
                    .lore(Component.text("Whether to enable drop protection or not.", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

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

        addItems(
                new SettingsButton(client, ClientProperty.TIPS_ENABLED, tipDescription),
                new SettingsButton(client, ClientProperty.CHAT_ENABLED, chatDescription),
                new SettingsButton(client, ClientProperty.DROP_PROTECTION_ENABLED, dropDescription),
                new SettingsButton(client, ClientProperty.COOLDOWN_SOUNDS_ENABLED, cooldownSoundsDescription)
        );
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("General Settings");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.REDSTONE_TORCH)
                        .displayName(Component.text("General Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View your general settings", NamedTextColor.GRAY)).frameLore(true)
                        .build())
                .build();
    }
}
