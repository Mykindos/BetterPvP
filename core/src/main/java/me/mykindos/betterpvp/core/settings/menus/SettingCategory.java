package me.mykindos.betterpvp.core.settings.menus;

import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.description.Description;

/**
 * Represents a category in {@link SettingsMenu}
 */
public interface SettingCategory extends Windowed {

    /**
     * @return Get the description of this category (icon, name, lore, etc.)
     */
    Description getDescription();

}
