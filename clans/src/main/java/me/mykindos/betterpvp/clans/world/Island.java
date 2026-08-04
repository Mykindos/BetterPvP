package me.mykindos.betterpvp.clans.world;

import me.mykindos.betterpvp.clans.world.content.WorldContentBinding;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A bundle of {@link WorldContent} for one named, hand-authored world.
 * <p>
 * On construction it declares its content to the {@link WorldContentService}, which installs it whenever that world
 * loads and removes it when the world goes away — so an island never contains spawning logic, zone-building, or
 * prop/NPC factories. Subclasses declare only their content and which world it lives in.
 * <p>
 * Both {@link #content()} and {@link #worldName()} are read lazily (at load time, not construction), so subclasses may
 * set up their content fields after calling {@code super(...)}.
 * <p>
 * Content that belongs to <em>many</em> worlds — anything that should appear on every instanced island — does not need
 * an island at all: it registers its own {@link WorldContentBinding} with a broader
 * {@link me.mykindos.betterpvp.clans.world.content.WorldSelector selector}.
 */
public abstract class Island {

    protected Island(@NotNull WorldContentService contentService) {
        contentService.register(new WorldContentBinding(world -> world.getName().equals(worldName()), this::content));
    }

    /**
     * @return all content on this island. Adding content is a one-liner here.
     */
    public abstract @NotNull List<WorldContent> content();

    /**
     * @return the world this island lives in; defaults to the main world
     */
    public @NotNull String worldName() {
        return BPvPWorld.MAIN_WORLD_NAME;
    }

    /**
     * @return a short name for logging
     */
    public abstract @NotNull String name();

}
