package me.mykindos.betterpvp.clans.scene;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ClansSceneObjectFactory extends SceneObjectFactory {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    private ClansSceneObjectFactory(SceneObjectRegistry registry, ClanManager clanManager, ClientManager clientManager) {
        super("clans", registry);
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @Override
    public String[] getTypes() {
        return new String[] { "traveler" };
    }

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String type) {
        return switch (type) {
            // Chunk-managed: the backing entity is (re)spawned when the NPC's chunk loads, so it survives chunk cycling.
            case "traveler" -> spawn(
                    new TravelerNPC(this, "Travel Hub", "Tavish", "npc_librarian", clanManager, clientManager),
                    location, this::backingEntity);
            default -> throw new IllegalArgumentException("Invalid clans NPC type: " + type);
        };
    }
}
