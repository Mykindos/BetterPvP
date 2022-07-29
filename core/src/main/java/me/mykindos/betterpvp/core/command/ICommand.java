package me.mykindos.betterpvp.core.command;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.entity.Player;

public interface ICommand {

    String getName();

    String[] getAliases();

    String getDescription();

    void execute(Player player, Client client, String... args);

    default Rank getRequiredRank() {
        return Rank.PLAYER;
    }

    default boolean informInsufficientRank() {
        return false;
    }

}
