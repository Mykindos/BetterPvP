package me.mykindos.betterpvp.core.settings.menus;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.settings.menus.buttons.EnumSettingsButton;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class GeneralSettingsMenu extends AbstractGui implements SettingCategory {

    public GeneralSettingsMenu(final Player player, final Client client, Windowed previous) {
        super(9, 2);
        final Gamer gamer = client.getGamer();

        final Description tipDescription = Description.builder().icon(lang -> {
            final boolean tipSetting = (boolean) client.getProperty(ClientProperty.TIPS_ENABLED).orElse(false);
            final NamedTextColor color = tipSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Translations.component("core.menu.settings.general.button.tips.name").color(color))
                    .lore(Translations.component("core.menu.settings.general.button.tips.lore.1").color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description chatDescription = Description.builder().icon(lang -> {
            final boolean chatSetting = (boolean) client.getProperty(ClientProperty.CHAT_ENABLED).orElse(false);
            final NamedTextColor color = chatSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Translations.component("core.menu.settings.general.button.chat.name").color(color))
                    .lore(Translations.component("core.menu.settings.general.button.chat.lore.1").color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description dropDescription = Description.builder().icon(lang -> {
            final boolean chatSetting = (boolean) client.getProperty(ClientProperty.DROP_PROTECTION_ENABLED).orElse(false);
            final NamedTextColor color = chatSetting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WRITABLE_BOOK)
                    .displayName(Translations.component("core.menu.settings.general.button.drop-protection.name").color(color))
                    .lore(Translations.component("core.menu.settings.general.button.drop-protection.lore.1").color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description cooldownSoundsDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ClientProperty.COOLDOWN_SOUNDS_ENABLED).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.JUKEBOX)
                    .displayName(Translations.component("core.menu.settings.general.button.sounds.name").color(color))
                    .lore(Translations.component("core.menu.settings.general.button.sounds.lore.1").color(NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        final Description tagDescription = Description.builder().icon(lang -> {
            final Rank.ShowTag showTag = Rank.ShowTag.valueOf((String) client.getProperty(ClientProperty.SHOW_TAG).orElse("NONE"));
            return ItemView.builder()
                    .material(Material.NAME_TAG)
                    .displayName(Translations.component("core.menu.settings.general.button.show-tag.name",
                            Component.text(showTag.name(), NamedTextColor.YELLOW)))
                    .lore(List.of(
                            Translations.component("core.menu.settings.general.button.show-tag.lore.1").color(NamedTextColor.GRAY),
                            Translations.component("core.menu.settings.general.button.show-tag.lore.2").color(NamedTextColor.GRAY),
                            Translations.component("core.menu.settings.general.button.show-tag.lore.3").color(NamedTextColor.GRAY),
                            Translations.component("core.menu.settings.general.button.show-tag.lore.4").color(NamedTextColor.GRAY)
                    ))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();

        fill(0, 9, Menu.BACKGROUND_GUI_ITEM, false);
        setItem(4, new BackButton(previous));

        addItems(
                new SettingsButton(client, ClientProperty.TIPS_ENABLED, tipDescription),
                new SettingsButton(client, ClientProperty.CHAT_ENABLED, chatDescription),
                new SettingsButton(client, ClientProperty.DROP_PROTECTION_ENABLED, dropDescription),
                new SettingsButton(client, ClientProperty.COOLDOWN_SOUNDS_ENABLED, cooldownSoundsDescription)
        );
        if (client.hasRank(Rank.YOUTUBE)) {
            addItems(new EnumSettingsButton(client, ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT, tagDescription));
        }
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Translations.component("core.menu.settings.general.title");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.REDSTONE_TORCH)
                        .displayName(Translations.component("core.menu.settings.general.description.name").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                        .lore(Translations.component("core.menu.settings.general.description.lore.1").color(NamedTextColor.GRAY)).frameLore(true)
                        .build())
                .build();
    }
}
