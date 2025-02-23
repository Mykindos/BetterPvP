package me.mykindos.betterpvp.clans.commands.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public abstract class ClanBrigadierCommand extends BrigadierCommand {
    protected final ClanManager clanManager;
    protected ClanBrigadierCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager);
        this.clanManager = clanManager;
    }

    protected boolean executorHasAClan(CommandSourceStack stack) {
        if (stack.getSender() instanceof final Player player) {
            return clanManager.getClanByPlayer(player).isPresent();
        }
        return false;
    }


    //Since we cannot throw a CommandSyntaxException in async contexts, this will pseudo throw on an empty optional.

    /**
     * Gets the requested Clan by Client, or informs the CommandSender that it does not exist
     * If the optional is empty, the client will be informed that a clan does not exist
     * @param client the client
     * @param commandSender the player sending the command
     */
    protected Optional<Clan> getClanByClient(Client client, CommandSender commandSender) {
        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(client.getUniqueId());
            if (clanOptional.isEmpty()) {
                commandSender
                        .sendMessage(UtilMessage.deserialize("<red>" + ClanArgument.NOT_IN_A_CLAN_EXCEPTION.create(client.getName())
                                .getMessage()));
            }
            return clanOptional;
    }
}
