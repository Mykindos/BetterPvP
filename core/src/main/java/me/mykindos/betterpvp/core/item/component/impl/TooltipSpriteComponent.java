package me.mykindos.betterpvp.core.item.component.impl;

import lombok.Value;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Optional component supplying a sprite glyph to render above the item's centered name in
 * the tooltip. Items opt in by declaring it on their {@link me.mykindos.betterpvp.core.item.BaseItem}
 * via {@code addBaseComponent(...)}. Not every item has a sprite.
 * <p>
 * This is render-time only data: it is added as a base component (not a serializable one),
 * so it is never written to the item's persistent data.
 */
@Value
public class TooltipSpriteComponent implements ItemComponent {

    private static final NamespacedKey KEY = new NamespacedKey("core", "tooltip_sprite");

    @NotNull Component sprite;

    /**
     * Builds a sprite component from a glyph in the shared {@link Resources.Font#SPRITE} font.
     *
     * @param glyph the sprite codepoint(s) in the {@code betterpvp:sprite} font
     * @return a component carrying the rendered sprite glyph
     */
    public static TooltipSpriteComponent of(@NotNull String glyph) {
        return new TooltipSpriteComponent(Component.text(glyph).font(Resources.Font.SPRITE));
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return KEY;
    }

    @Override
    public ItemComponent copy() {
        return new TooltipSpriteComponent(sprite);
    }
}
