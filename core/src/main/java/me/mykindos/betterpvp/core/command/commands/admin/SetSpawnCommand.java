package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.entity.Player;

@WithReflection
public class SetSpawnCommand extends Command {

    private final Core core;

    @Inject
    public SetSpawnCommand(Core core) {
        this.core = core;
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
        player.getWorld().setSpawnLocation(player.getLocation());
        UtilMessage.simpleMessage(player, "Spawn", "You set Spawn at <yellow>%s<gray>.", UtilWorld.locationToString(player.getLocation()));
    }
}
