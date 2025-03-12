package me.mykindos.betterpvp.game.guice.provider;

import com.google.inject.Inject;
import jakarta.inject.Provider;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;

public class WaitingLobbyProvider implements Provider<MappedWorld> {

    private final MapManager manager;

    @Inject
    public WaitingLobbyProvider(MapManager manager) {
        this.manager = manager;
    }

    @Override
    public MappedWorld get() {
        return manager.getWaitingLobby();
    }
}
