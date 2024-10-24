package me.mykindos.betterpvp.core.settings.menus.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import org.bukkit.entity.Player;

import java.util.LinkedList;

/**
 * Called when the {@link SettingsMenu} is initialized.
 */
@Getter
@RequiredArgsConstructor
public class SettingsFetchEvent extends CustomEvent {

    private final Player player;
    private final Client client;
    private final LinkedList<SettingCategory> categories = new LinkedList<>();
    private final Windowed settingsMenu;

    public void supply(SettingCategory category) {
        categories.add(category);
    }

}
