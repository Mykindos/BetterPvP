package me.mykindos.betterpvp.game.framework.model.team;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.format.TextColor;

/**
 * Represents a team configuration, with:
 * <ul>
 *     <li>Size</li>
 *     <li>Name</li>
 *     <li>Color</li>
 * </ul>
 */
public record TeamProperties(int size, String name, TextColor color, boolean friendlyFire) {

    public TeamProperties {
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        Preconditions.checkArgument(size > 0, "size must be greater than 0");
    }

}
