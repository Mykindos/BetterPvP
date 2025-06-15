package me.mykindos.betterpvp.core.item;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Represents the rarity of an item.
 */
public enum ItemRarity {

    COMMON(0, "Common", NamedTextColor.WHITE, false),
    UNCOMMON(1, "Uncommon", NamedTextColor.GREEN, false),
    RARE(2, "Rare", NamedTextColor.LIGHT_PURPLE, false),
    EPIC(3, "Epic", NamedTextColor.DARK_PURPLE, false),
    LEGENDARY(4, "Legendary", NamedTextColor.RED, true),
    MYTHIC(5, "Mythic", NamedTextColor.DARK_RED, true);

    private final int importance;
    private final String name;
    private final TextColor color;
    private final boolean isImportant;

    ItemRarity(int importance, String name, TextColor color, boolean isImportant) {
        this.importance = importance;
        this.name = name;
        this.color = color;
        this.isImportant = isImportant;
    }

    public int getImportance() {
        return importance;
    }

    public String getName() {
        return name;
    }

    public TextColor getColor() {
        return color;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public boolean isAtLeast(ItemRarity rarity) {
        return this.ordinal() >= rarity.ordinal();
    }

}
