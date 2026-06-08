package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.WorldHandler;
import net.kyori.adventure.text.Component;
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
        return "core.command.set-spawn.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            player.getWorld().setSpawnLocation(player.getLocation());
            UtilMessage.message(player, "core.prefix.spawn", "core.command.setspawn.main.success");
            return;
        }

        String spawnName = String.join(" ", args);

        worldHandler.getSpawnLocations().put(spawnName, player.getLocation());
        core.getConfig().set("spawns." + spawnName, UtilWorld.locationToString(player.getLocation(), false));
        core.saveConfig();

        UtilMessage.message(player, "core.prefix.spawn", Translations.component("core.command.setspawn.named.success",
                Component.text(spawnName),
                Component.text(UtilWorld.locationToString(player.getLocation()))));
    }
}
