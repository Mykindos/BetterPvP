package me.mykindos.betterpvp.game.guice.provider;

import com.google.inject.Inject;
import jakarta.inject.Provider;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;

public class CurrentMapProvider implements Provider<MappedWorld> {

    private final MapManager manager;

    @Inject
    public CurrentMapProvider(MapManager manager) {
        this.manager = manager;
    }

    @Override
    public MappedWorld get() {
        return manager.getCurrentMap();
    }
}
