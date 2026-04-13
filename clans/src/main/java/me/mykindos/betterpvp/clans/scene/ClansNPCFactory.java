package me.mykindos.betterpvp.clans.scene;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.scene.npc.NPCFactory;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ClansNPCFactory extends NPCFactory {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    private ClansNPCFactory(SceneObjectRegistry registry, ClanManager clanManager, ClientManager clientManager) {
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
            case "traveler" -> spawnNPC(
                    new TravelerNPC(this, "Travel Hub", "Tavish", "npc_librarian", clanManager, clientManager),
                    backingEntity(location));
            default -> throw new IllegalArgumentException("Invalid clans NPC type: " + type);
        };
    }

    private Entity backingEntity(@NotNull Location location) {
        return location.getWorld().spawn(location, Pig.class, spawned -> {
            spawned.setAI(false);
            spawned.setInvulnerable(true);
            spawned.setCollidable(false);
            spawned.setPersistent(false);
            spawned.setInvisible(true);
            spawned.setSilent(true);
        });
    }
}
