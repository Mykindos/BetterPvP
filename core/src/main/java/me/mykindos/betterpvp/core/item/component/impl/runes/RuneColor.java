package me.mykindos.betterpvp.core.item.component.impl.runes;

/**
 * Represents the color of a rune.
 */
public enum RuneColor {

    RED,
    BLUE,
    YELLOW,
    PURPLE;

    public static RuneColor WEAPON = RED;
    public static RuneColor ARMOR = BLUE;
    public static RuneColor TOOL = YELLOW;
    public static RuneColor MISC = PURPLE;

    public String getModelData() {
        return this.name().toLowerCase();
    }

}
