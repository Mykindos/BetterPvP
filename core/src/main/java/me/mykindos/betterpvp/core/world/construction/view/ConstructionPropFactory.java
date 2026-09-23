package me.mykindos.betterpvp.core.world.construction.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/** Owns the scene objects standing in for structures. They come from holdings, never from a command. */
@Singleton
public class ConstructionPropFactory extends SceneObjectFactory {

    @Inject
    public ConstructionPropFactory(@NotNull SceneObjectRegistry registry) {
        super("construction", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("Structures are placed through construction, not spawned by command");
    }
}
