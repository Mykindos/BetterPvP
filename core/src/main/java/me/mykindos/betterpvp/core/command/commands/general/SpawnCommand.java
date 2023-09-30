package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.events.SpawnTeleportEvent;
import org.bukkit.entity.Player;

@Singleton
public class SpawnCommand extends Command {

    private final WorldHandler worldHandler;

    @Inject
    public SpawnCommand(WorldHandler worldHandler) {
        this.worldHandler = worldHandler;
    }


    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Teleport to spawn";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilServer.callEvent(new SpawnTeleportEvent(player, () -> player.teleport(worldHandler.getSpawnLocation())));
    }
}
