package me.mykindos.betterpvp.clans.world.content;

import lombok.Value;
import me.mykindos.betterpvp.clans.world.WorldContent;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * A bundle of {@link WorldContent} paired with the worlds it belongs to.
 * <p>
 * The content is supplied rather than held so it is resolved at load time, which lets a bundle be assembled from
 * services that are not ready when the binding is declared, and lets a reload pick up a changed list.
 */
@Value
public class WorldContentBinding {

    WorldSelector selector;
    Supplier<List<WorldContent>> content;

    public boolean matches(@NotNull World world) {
        return selector.matches(world);
    }
}
