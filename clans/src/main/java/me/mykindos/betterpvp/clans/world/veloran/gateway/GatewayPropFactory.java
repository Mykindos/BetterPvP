package me.mykindos.betterpvp.clans.world.veloran.gateway;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Satisfies the {@link me.mykindos.betterpvp.core.scene.prop.Prop Prop}↔{@link SceneObjectFactory} pairing for
 * {@link GatewayProp}.
 * <p>
 * The Sundered Gate is defined by a Mapper <em>cuboid</em> (the portal volume), not a single point, so it is spawned
 * by {@link SunderedGate} from map data rather than via the point-based {@code /prop spawn} command. This factory is
 * therefore intentionally not registered with the {@code SceneObjectFactoryManager} and rejects command spawns.
 */
@Singleton
public class GatewayPropFactory extends SceneObjectFactory {

    @Inject
    private GatewayPropFactory(SceneObjectRegistry registry) {
        super("gateway", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("The Sundered Gate is loaded from its Mapper cuboid region, not spawned by command");
    }
}
