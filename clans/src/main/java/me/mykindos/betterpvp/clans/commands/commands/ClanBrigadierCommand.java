package me.mykindos.betterpvp.clans.commands.commands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.ClanArgument;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import org.bukkit.command.CommandSender;

import java.util.Objects;
import java.util.Optional;

public abstract class ClanBrigadierCommand extends BrigadierCommand {
    protected final ClanManager clanManager;
    protected ClanBrigadierCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager);
        this.clanManager = clanManager;
    }


    //Since we cannot throw a CommandSyntaxException in async contexts, this will pseudo throw on an empty optional.

    /**
     * Gets the requested Clan by Client, or informs the CommandSender that it does not exist
     * If the optional is empty, the client will be informed that a clan does not exist
     * @param client the client
     * @param commandSender the player sending the command
     */
    protected Optional<Clan> getClanByClient(Client client, CommandSender commandSender) {
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(client.getUniqueId());
            if (clanOptional.isEmpty()) {
                commandSender
                        .sendMessage(Objects.requireNonNull(ClanArgument.NOTINACLANEXCEPTION.create(client.getName())
                                .componentMessage()));
            }
            return clanOptional;
    }
}
