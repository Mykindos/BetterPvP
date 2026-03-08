package me.mykindos.betterpvp.core.item.component.impl.purity;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents the purity level of an item.
 * Purity affects socket count and stat variance for items.
 */
@Getter
public enum ItemPurity {
    PITIFUL(0, "Pitiful", NamedTextColor.GRAY),
    FRAGILE(1, "Fragile", NamedTextColor.WHITE),
    MODERATE(2, "Moderate", NamedTextColor.GREEN),
    POLISHED(3, "Polished", NamedTextColor.BLUE),
    PRISTINE(4, "Pristine", NamedTextColor.LIGHT_PURPLE),
    PERFECT(5, "Perfect", NamedTextColor.GOLD);

    private final int level;
    private final String displayName;
    private final NamedTextColor color;

    ItemPurity(int level, String displayName, NamedTextColor color) {
        this.level = level;
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Get the purity level by ordinal.
     *
     * @param level The level (0-5)
     * @return The corresponding ItemPurity
     * @throws IllegalArgumentException if level is out of range
     */
    @NotNull
    public static ItemPurity fromLevel(int level) {
        for (ItemPurity purity : values()) {
            if (purity.level == level) {
                return purity;
            }
        }
        throw new IllegalArgumentException("Invalid purity level: " + level);
    }

    /**
     * Creates a formatted lore component displaying the purity information.
     * Format: "Purity: <DisplayName> (⭐⭐⭐)"
     *
     * @return The formatted lore component
     */
    @NotNull
    public Component createLoreComponent() {
        final TextComponent stars = Component.text("⭐".repeat(this.level + 1), NamedTextColor.YELLOW);
        final TextColor foreground = Objects.requireNonNull(TextColor.fromHexString("#828282"));

        return Component.empty()
                .append(Component.text("Purity:", foreground))
                .appendSpace()
                .append(Component.text(this.displayName, this.color, TextDecoration.UNDERLINED))
                .appendSpace()
                .append(Component.text("(", foreground))
                .append(stars)
                .append(Component.text(")", foreground))
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Gets the name color override for this purity level.
     * Returns null if no override should be applied (use rarity color).
     * Currently only PERFECT purity overrides the name color.
     *
     * @return The text color to use for the item name, or null for default (rarity color)
     */
    @Nullable
    public TextColor getNameColorOverride() {
        if (this == PERFECT) {
            return TextColor.fromHexString("#e6fffe");
        }
        return null;
    }
}
