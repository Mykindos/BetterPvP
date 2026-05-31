package me.mykindos.betterpvp.clans.world.veloran;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.Continent;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.clans.world.veloran.gateway.SunderedGate;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
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
public class Veloran extends Continent {

    private final SunderedGate sunderedGate;

    @Inject
    public Veloran(ZoneManager zoneManager, SceneObjectRegistry sceneRegistry, SceneLoaderManager loaderManager,
                   Clans clans, SunderedGate sunderedGate) {
        super(zoneManager, sceneRegistry, loaderManager, clans);
        this.sunderedGate = sunderedGate;
    }

    @Override
    public @NotNull String worldName() {
        return "Clans_Spawn_New";
    }

    @Override
    public @NotNull List<WorldContent> content() {
        return List.of(sunderedGate);
    }
}
