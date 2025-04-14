package me.mykindos.betterpvp.clans.commands.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link Clans} {@link BrigadierCommand}, with some common methods using {@link ClanManager}
 */
public abstract class ClanBrigadierCommand extends BrigadierCommand {
    protected final ClanManager clanManager;
    protected ClanBrigadierCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager);
        this.clanManager = clanManager;
    }

    /**
     * checks if the {@link CommandSourceStack#getExecutor() executor} has a {@link Clan}
     * @param stack the {@link CommandSourceStack}
     * @return {@code true} if the {@link CommandSourceStack#getExecutor() executor} is in a {@link Clan},
     * {@code false} otherwise
     */
    protected boolean executorHasAClan(@NotNull CommandSourceStack stack) {
        if (stack.getExecutor() instanceof final Player player) {
            return clanManager.getClanByPlayer(player).isPresent();
        }
        return false;
    }


    /**
     * Gets the {@link Clan} by the {@link CommandSourceStack#getExecutor() executor}
     * @param context the {@link CommandContext}
     * @return the {@link Clan} of the {@link CommandSourceStack#getExecutor() executor}
     * @throws CommandSyntaxException if the {@link CommandSourceStack#getExecutor() executor} is not in a {@link Clan}
     * @see BrigadierCommand#getPlayerFromExecutor(CommandContext)
     */
    @NotNull
    protected Clan getClanByExecutor(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final Player player = getPlayerFromExecutor(context);
        return clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

    }


    //Since we cannot throw a CommandSyntaxException in async contexts, this will pseudo throw on an empty optional.

    /**
     * Gets the requested Clan by Client, or informs the CommandSender that it does not exist
     * If the optional is empty, the client will be informed that a clan does not exist
     * @param client the client
     * @param commandSender the player sending the command
     */
    protected Optional<Clan> getClanByClient(@NotNull Client client, @NotNull CommandSender commandSender) {
        final Optional<Clan> clanOptional = clanManager.getClanByClient(client);
            if (clanOptional.isEmpty()) {
                UtilMessage.sendCommandSyntaxException(commandSender, ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(client.getName()));
            }
            return clanOptional;
    }
}
