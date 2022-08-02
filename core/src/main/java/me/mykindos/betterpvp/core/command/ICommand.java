package me.mykindos.betterpvp.core.command;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface ICommand {

    String getName();

    List<String> getAliases();

    String getDescription();

    void execute(Player player, Client client, String... args);

    default Rank getRequiredRank() {
        return Rank.PLAYER;
    }

    default boolean informInsufficientRank() {
        return true;
    }

    boolean isEnabled();

    List<String> processTabComplete(CommandSender sender, String[] args);

    default ArgumentType getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND;
    }


    enum ArgumentType {
        NONE,
        SUBCOMMAND,
        PLAYER,
        POSITION_X,
        POSITION_Y,
        POSITION_Z,
        CLAN
    }

}
