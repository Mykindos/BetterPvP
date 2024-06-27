package me.mykindos.betterpvp.core.wiki.types;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a class that has functions to display static things in a menu
 */
public interface IStaticWikiable {

    List<String> getWikiDescription();
    String getTitle();
    Material getDisplayMaterial();
    default int getDisplayModelData() {
        return 0;
    }

    default int getAmount() {
        return 1;
    }


}
