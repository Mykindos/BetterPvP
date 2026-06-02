package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Singleton
public class HubNPCFactory extends SceneObjectFactory {

    @Inject
    private HubNPCFactory(SceneObjectRegistry registry) {
        super("hub", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("Hub NPCs are managed by HubSceneLoader");
    }
}
