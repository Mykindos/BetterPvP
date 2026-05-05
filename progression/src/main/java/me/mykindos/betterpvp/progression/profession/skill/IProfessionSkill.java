package me.mykindos.betterpvp.progression.profession.skill;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

public interface IProfessionSkill {

    String getName();

    String[] getDescription(int level);

    Material getIcon();

    default boolean isGlowing() {
        return false;
    }

    default ItemFlag getFlag() {
        return null;
    }
}
