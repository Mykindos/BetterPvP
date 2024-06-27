package me.mykindos.betterpvp.core.wiki.types;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a class that has functions to display a leveled thing in a menu
 */
public interface ILevelWikiable {

    List<String> getWikiDescription(int level);
    int getLevel();
    String getTitle(int level);
    Material getDisplayMaterial(int level);
    default int getDisplayModelData(int level) {
        return 0;
    }

    default int amount(int level) {
        return level;
    }


}