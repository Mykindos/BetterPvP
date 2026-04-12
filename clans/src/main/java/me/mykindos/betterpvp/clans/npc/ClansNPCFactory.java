package me.mykindos.betterpvp.clans.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ClansNPCFactory extends NPCFactory {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    private ClansNPCFactory(NPCRegistry registry, ClanManager clanManager, ClientManager clientManager) {
        super("clans", registry);
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @Override
    public String[] getTypes() {
        return new String[] {
                "traveler"
        };
    }

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String type) {
        final NPC npc = switch (type) {
            case "traveler" -> new TravelerNPC(this, backingEntity(location), "Travel Hub", "Tavish", "npc_librarian", clanManager, clientManager);
            default -> throw new IllegalArgumentException("Invalid clans NPC type: " + type);
        };
        registry.register(npc);
        return npc;
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
