package me.mykindos.betterpvp.game.framework.model.team;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;

/**
 * Represents a team configuration, with:
 * <ul>
 *     <li>Size</li>
 *     <li>Name</li>
 *     <li>Color</li>
 * </ul>
 */
public record TeamProperties(int size, String name, TextColor color, TextColor secondary, DyeColor vanillaColor, boolean friendlyFire) {

    public TeamProperties {
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        Preconditions.checkArgument(size > 0, "size must be greater than 0");
    }

    public static TeamProperties defaultRed(int size) {
        return new TeamProperties(size,
                "Red",
                TextColor.color(255, 51, 51),
                TextColor.color(255, 122, 122),
                DyeColor.RED,
                false);
    }

    public static TeamProperties defaultBlue(int size) {
        return new TeamProperties(size,
                "Blue",
                TextColor.color(38, 111, 255),
                TextColor.color(122, 204, 255),
                DyeColor.BLUE,
                false);
    }

}
