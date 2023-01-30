package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class TeleportHereCommand extends Command {

    @WithReflection
    public TeleportHereCommand(){
        aliases.add("tphere");
    }

    @Override
    public String getName() {
        return "teleporthere";
    }

    @Override
    public String getDescription() {
        return "Teleport a target player to yourself";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length > 0){
            Player target = Bukkit.getPlayer(args[0]);
            if(target != null){
                target.teleport(player.getLocation());
            }
        }
    }

    @Override
    public String getArgumentType(int arg) {
        return ArgumentType.PLAYER.name();
    }

}
