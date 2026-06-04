package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link SceneObjectFactory} backing {@link ResourceNodeProp}s. Resource nodes are loaded from Mapper regions by
 * {@link ResourceNodeLoader}, not spawned by command, so {@link #spawnDefault} is unsupported — the factory exists
 * only to satisfy the prop's two-phase init/registration contract.
 */
@Singleton
public class ResourceNodeFactory extends SceneObjectFactory {

    @Inject
    public ResourceNodeFactory(@NotNull SceneObjectRegistry registry) {
        super("resourcenode", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("Resource nodes are loaded from Mapper regions, not spawned by command");
    }
}
