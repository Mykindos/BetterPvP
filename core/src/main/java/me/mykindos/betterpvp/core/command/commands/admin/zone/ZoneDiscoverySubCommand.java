package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code /zone discovery <list|add|remove|reset> ...} — groups the zone-discovery management subcommands under
 * {@code /zone}. Bare invocation prints usage; tab-completion descends into the matching leaf (the command framework
 * only auto-descends one level, so this group descends to its own children explicitly).
 */
@Singleton
@SubCommand(ZoneCommand.class)
public class ZoneDiscoverySubCommand extends Command {

    @Override
    public String getName() {
        return "discovery";
    }

    @Override
    public String getDescription() {
        return "Manage players' zone discoveries";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Zones", "<yellow>Usage:</yellow> /zone discovery <list|add|remove|reset> <player>");
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            final String arg = args.length == 1 ? args[0].toLowerCase() : "";
            final List<String> completions = new ArrayList<>();
            for (ICommand sub : getSubCommands()) {
                if (sub.showTabCompletion(sender) && sub.getName().toLowerCase().startsWith(arg)) {
                    completions.add(sub.getName());
                }
            }
            return completions;
        }
        return getSubCommand(args[0])
                .filter(sub -> sub.showTabCompletion(sender))
                .map(sub -> sub.processTabComplete(sender, Arrays.copyOfRange(args, 1, args.length)))
                .orElseGet(ArrayList::new);
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND.name();
    }
}
