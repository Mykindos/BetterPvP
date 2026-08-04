package me.mykindos.betterpvp.clans.world.veloran;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.Continent;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.content.WorldContentService;
import me.mykindos.betterpvp.clans.world.veloran.gateway.SunderedGate;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The Veloran continent. Bundles all Veloran content — currently just {@link SunderedGate The Sundered Gate} — and
 * lets the generic {@link Continent} loaders install it. Adding Veloran content is a single entry in {@link #content()}.
 * <p>
 * Discovered and instantiated as a {@code Mapper} {@link PluginAdapter}; the {@link Continent} constructor wires up its
 * zone and scene loaders.
 */
@Singleton
@PluginAdapter("Mapper")
public class Veloran extends Island implements Continent {

    private final SunderedGate sunderedGate;

    @Inject
    public Veloran(@NotNull WorldContentService contentService, SunderedGate sunderedGate) {
        super(contentService);
        this.sunderedGate = sunderedGate;
    }

    @Override
    public @NotNull String worldName() {
        return "Clans_Spawn_New";
    }

    @Override
    public @NotNull String name() {
        return "Veloran";
    }

    @Override
    public @NotNull List<WorldContent> content() {
//        return List.of(sunderedGate);
        return List.of();
    }
}
