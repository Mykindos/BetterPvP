package me.mykindos.betterpvp.core.world.content;

import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Value;
import lombok.With;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorldContentBinding {

    WorldSelector selector;
    Supplier<List<WorldContent>> content;

    /**
     * Holds the content back until ModelEngine has registered its models, and applies it again every time it does, so
     * anything wearing a model never spawns without one.
     */
    @With
    boolean requiresModels;

    /** Runs before the content is applied again for a module reload, for content that caches what it read. */
    @With
    Runnable onReload;

    public WorldContentBinding(@NotNull WorldSelector selector, @NotNull Supplier<List<WorldContent>> content) {
        this(selector, content, false, () -> {});
    }

    public boolean matches(@NotNull World world) {
        return selector.matches(world);
    }
}
