package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.WorldHandler;
import org.bukkit.entity.Player;

@Singleton
public class SetSpawnCommand extends Command {

    private final Core core;
    private final WorldHandler worldHandler;

    @Inject
    public SetSpawnCommand(Core core, WorldHandler worldHandler) {
        this.core = core;
        this.worldHandler = worldHandler;
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "Set the spawn point for the current world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            player.getWorld().setSpawnLocation(player.getLocation());
            UtilMessage.message(player, "Spawn", "Set the main world spawn point to your current location.");
            return;
        }

        String spawnName = String.join(" ", args);

        worldHandler.getSpawnLocations().put(spawnName, player.getLocation());
        core.getConfig().set("spawns." + spawnName, UtilWorld.locationToString(player.getLocation(), false));
        core.saveConfig();

        UtilMessage.simpleMessage(player, "Spawn", "You created a spawn (<green>%s</green>) at <yellow>%s<gray>.",
                spawnName, UtilWorld.locationToString(player.getLocation()));
    }
}
