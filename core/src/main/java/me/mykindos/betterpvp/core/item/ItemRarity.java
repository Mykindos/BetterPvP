package me.mykindos.betterpvp.core.item;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Represents the rarity of an item.
 */
public enum ItemRarity {

    COMMON(0, "Common", NamedTextColor.WHITE, false, '\uE00B'),
    UNCOMMON(1, "Uncommon", NamedTextColor.GREEN, false, '\uE03B'),
    RARE(2, "Rare", TextColor.color(74, 103, 255), false, '\uE02F'),
    EPIC(3, "Epic", NamedTextColor.DARK_PURPLE, false, '\uE013'),
    LEGENDARY(4, "Legendary", NamedTextColor.GOLD, true, '\uE01E'),
    MYTHICAL(5, "Mythical", NamedTextColor.DARK_RED, true, '\uE028');

    private final int importance;
    private final String name;
    private final TextColor color;
    private final boolean isImportant;
    private final char glyph;

    ItemRarity(int importance, String name, TextColor color, boolean isImportant, char glyph) {
        this.importance = importance;
        this.name = name;
        this.color = color;
        this.isImportant = isImportant;
        this.glyph = glyph;
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

    public char getGlyph() {
        return glyph;
    }

    public boolean isAtLeast(ItemRarity rarity) {
        return this.ordinal() >= rarity.ordinal();
    }

}
