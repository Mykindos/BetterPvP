package me.mykindos.betterpvp.core.world.settler.presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/** Owns settler bodies. They come from rosters, never from a command. */
@Singleton
public class SettlerFactory extends SceneObjectFactory {

    @Inject
    public SettlerFactory(@NotNull SceneObjectRegistry registry) {
        super("settler", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public SceneObject spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("Settlers come from rosters, not from a command");
    }
}
