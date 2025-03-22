package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class CenterSelfCommand extends Command {

    @WithReflection
    public CenterSelfCommand() {
    }

    @Override
    public String getName() {
        return "centerself";
    }

    @Override
    public String getDescription() {
        return "center yourself on the specified block";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Location location = player.getLocation().clone().toCenterLocation();
        location.setY(location.getBlockY());
        if (args.length > 0) {
            String direction = args[0].toUpperCase();
            switch (direction) {
                case "NORTH" -> {
                    location.setRotation(-180, 0);
                    break;
                }
                case "SOUTH" -> {
                    location.setRotation(0, 0);
                    break;
                }
                case "EAST" -> {
                    location.setRotation(-90, 0);
                    break;
                }
                case "WEST" -> {
                    location.setRotation(90, 0);
                    break;
                }
                default -> {
                    location.setRotation(0, 90);
                }
            }
        } else {
            location.setRotation(0, 90);
        }

        player.teleport(location);
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? "DIRECTION" : ArgumentType.NONE.name();
    }


    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);

        List<String> directions = List.of("NORTH", "SOUTH", "EAST", "WEST");

        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("DIRECTION")) {
            tabCompletions.addAll(directions.stream()
                    .filter(name -> name.toLowerCase().startsWith(lowercaseArg)).toList());
        }

        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
