package me.mykindos.betterpvp.core.settings.menus.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
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
    private final Gamer gamer;
    private final LinkedList<SettingCategory> categories = new LinkedList<>();

    public void supply(SettingCategory category) {
        categories.add(category);
    }

}
