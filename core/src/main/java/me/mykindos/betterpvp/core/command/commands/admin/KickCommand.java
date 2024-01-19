package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineHuman;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@Singleton
public class KickCommand extends Command implements IConsoleCommand {

    @Inject
    private ClientManager clientManager;

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kick a player from the server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            UtilMessage.message(sender, "Command", "You must specify a player and a reason");
            return;
        }

        final SearchEngineHuman<Client> search = clientManager.search(sender);
        final Collection<Client> matches = search.advancedOnline(args[0]);
        if (matches.size() != 1) {
            return;
        }

        final Client target = matches.iterator().next();
        final Player toKick = Objects.requireNonNull(target.getGamer().getPlayer());
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        toKick.kick(UtilMessage.deserialize("<red>[Kick] <gray>" + reason), PlayerKickEvent.Cause.KICK_COMMAND);
        UtilMessage.simpleBroadcast("Kick", "<alt2>%s</alt2> kicked <alt2>%s</alt2> for <alt>%s</alt>", sender.getName(), target.getName(), reason);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getArgumentType(int argCount) {
        if(argCount == 1){
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }



}
