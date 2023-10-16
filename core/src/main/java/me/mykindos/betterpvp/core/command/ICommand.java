package me.mykindos.betterpvp.core.command;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ICommand {

    String getName();

    List<String> getAliases();

    String getDescription();

    void execute(Player player, Client client, String... args);

    default void process(Player player, Client client, String... args) {
        execute(player, client, args);
    }

    Rank getRequiredRank();
    void setRequiredRank(Rank rank);

    default boolean informInsufficientRank() {
        return true;
    }

    boolean isEnabled();
    void setEnabled(boolean enabled);

    Optional<ICommand> getSubCommand(String name);
    List<ICommand> getSubCommands();

    default boolean showTabCompletion(CommandSender sender) {
        return true;
    }

    default List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if(args.length == 0) return tabCompletions;

        switch (getArgumentType(args.length)) {
            case "PLAYER" -> tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                    startsWith(args[args.length-1].toLowerCase())).toList());
            case "POSITION_X" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getX() + "" : "0");
            case "POSITION_Y" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getY() + "" : "0");
            case "POSITION_Z" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getZ() + "" : "0");
            case "BOOLEAN" -> tabCompletions.addAll(List.of("true", "false"));
        }


        return tabCompletions;

    }

    default String getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND.name();
    }

    enum ArgumentType {
        NONE,
        SUBCOMMAND,
        PLAYER,
        POSITION_X,
        POSITION_Y,
        POSITION_Z,
        WORLD,
        BOOLEAN
    }

}
