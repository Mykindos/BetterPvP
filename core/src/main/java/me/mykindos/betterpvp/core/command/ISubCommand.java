package me.mykindos.betterpvp.core.command;

import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.entity.Player;

public interface ISubCommand {

    String getName();

    void execute(Player player, Client client, String[] args);

}
