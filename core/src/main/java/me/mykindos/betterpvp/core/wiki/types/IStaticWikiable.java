package me.mykindos.betterpvp.core.wiki.types;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a class that has functions to display static things in a menu
 */
public interface IStaticWikiable extends IWikiable {

    /**
     * a list of mini-message formatted strings, to display in as the wiki item
     * @return a list of strings, each string in the list is a line
     */
    List<String> getWikiDescription();

    /**
     * The mini-message formatted title
     * @return
     */
    String getTitle();

    /**
     * The base material to use as the item in the wiki
     * @return
     */
    Material getDisplayMaterial();

    /**
     * An optional model data for the specified material, default is 0
     * @return
     */
    default int getDisplayModelData() {
        return 0;
    }

    /**
     * The number of items to display, default 1
     * @return
     */
    default int getAmount() {
        return 1;
    }

}
