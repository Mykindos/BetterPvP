package me.mykindos.betterpvp.clans.world.content;

import me.mykindos.betterpvp.clans.world.island.IslandWorldProvisioner;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Which worlds a piece of content belongs to.
 * <p>
 * Content used to be pinned to one hand-authored world by name, which meant it could never reach a world that only
 * exists at runtime - a discovery island cloned from a template has a name nobody can write down in advance. Selecting
 * worlds by rule rather than by name is what lets the same content be authored once and appear on every copy.
 */
@FunctionalInterface
public interface WorldSelector {

    boolean matches(@NotNull World world);

    /**
     * Every world. Content selected this way must be driven purely by the map data it finds, since it will be asked
     * about worlds it has never heard of - which is exactly what makes it plug-and-play.
     */
    static @NotNull WorldSelector any() {
        return world -> true;
    }

    static @NotNull WorldSelector named(@NotNull String... names) {
        final List<String> wanted = Arrays.asList(names);
        return world -> wanted.contains(world.getName());
    }

    /** Every live instance cloned from one island template. */
    static @NotNull WorldSelector islandTemplate(@NotNull String templateKey) {
        final String prefix = IslandWorldProvisioner.worldNamePrefix(templateKey);
        return world -> world.getName().startsWith(prefix);
    }
}
