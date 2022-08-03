package me.mykindos.betterpvp.core.command.commands.qol;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TeleportCommand extends Command {

    @WithReflection
    public TeleportCommand() {
        aliases.add("tp");
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return "Teleport to a target play";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        switch (args.length) {
            case 1 -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    player.teleport(target.getLocation());
                }
            }
            case 2 -> {
                Player fromPlayer = Bukkit.getPlayer(args[0]);
                Player toPlayer = Bukkit.getPlayer(args[1]);
                if (fromPlayer != null && toPlayer != null) {
                    fromPlayer.teleport(toPlayer.getLocation());
                }
            }
        }
    }

    @Override
    public String getArgumentType(int arg) {
        return ArgumentType.PLAYER.name();
    }
}
