package me.mykindos.betterpvp.core.wiki.types;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a class that has functions to display a leveled thing in a menu
 */
public interface ILevelWikiable {

    /**
     * Represents the wiki description
     * @param level the level
     * @return a mini-message formatted list of strings, having the correct description for the level
     */
    List<String> getWikiDescription(int level);

    /**
     *
     * @return the maximum level
     */
    int getMaxLevel();

    /**
     * The title to show
     * @param level the level
     * @return a mini-message formatted string to display
     */
    String getTitle(int level);

    /**
     * The base material to use as the item in the wiki for the current level
     * @param level the level
     * @return the material
     */
    Material getDisplayMaterial(int level);

    /**
     * An optional model data for the specified material and level, default is 0
     * @param level the level
     * @return
     */
    default int getDisplayModelData(int level) {
        return 0;
    }

    /**
     * The number of items to display, default level
     * @param level the level
     * @return number of items
     */
    default int amount(int level) {
        return level;
    }

    WikiCategory getCategory();

}