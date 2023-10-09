package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import javax.naming.Name;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ClanHelpSubCommand extends ClanSubCommand {

    private final ClanCommand clanCommand;
    @Inject
    public ClanHelpSubCommand(ClanManager clanManager, GamerManager gamerManager, ClanCommand clanCommand) {
        super(clanManager, gamerManager);
        aliases.addAll(List.of("?", "h"));

        this.clanCommand = clanCommand;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "List all available commands.";
    }

    @Override public String getUsage() {
        return super.getUsage() + " [pageNumber|commandName]";
    }


    @Override
    public void execute(Player player, Client client, String... args) {;
        int numPerPage = 10;
        int pageNumber = 1;
        String commandName = "";

        if (args.length == 1) {
            try {
                pageNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                commandName = args[0];
            }
        }

        List<ICommand> clanSubCommands = clanCommand.getSubCommands();

        Collections.sort(clanSubCommands, Comparator.comparing(ICommand::getName));



        Component component = UtilMessage.deserialize("<yellow>Help<grey>: ");

        if (!commandName.isEmpty()){
            Optional<ICommand> subCommandOptional = clanCommand.getSubCommand(commandName);
            if(subCommandOptional.isPresent()) {
                ICommand subCommand = subCommandOptional.get();
                if(subCommand instanceof ClanSubCommand clanSubCommand) {
                    component = component.append(Component.text(commandName, NamedTextColor.YELLOW))
                            .append(addHelpCommandComponent(clanSubCommand, client).appendNewline()
                            .append(Component.text("Usage: ", NamedTextColor.YELLOW))
                            .append(Component.text("/clan ", NamedTextColor.GRAY)).append(Component.text(clanSubCommand.getUsage(), NamedTextColor.GRAY)));
                }
            }
            else {
                component = Component.text("No Clan command named ", NamedTextColor.RED).append(Component.text(commandName, NamedTextColor.YELLOW).append(Component.text(" exists.", NamedTextColor.RED)));
            }
        }
        else {
            int count = 0;
            int start = (pageNumber - 1) * numPerPage;
            int end = start + numPerPage;
            int size = clanSubCommands.size();
            int totalPages = size /numPerPage;
            if (size % numPerPage > 0) {
                totalPages++;
            }

            component = component.append(UtilMessage.deserialize("<white>" + pageNumber + "<gray> / <white>" + totalPages));

            if (start <= size) {
                if (end > size) end = size;
                for (ICommand subCommand : clanSubCommands.subList(start, end)) {
                    if (count == numPerPage) break;
                    if (subCommand instanceof ClanSubCommand clanSubCommand) {
                        component = component.append(addHelpCommandComponent(clanSubCommand, client));
                    }
                    count++;
                }
            }
        }
        UtilMessage.message(player, "Clans", component);

    }

    private Component addHelpCommandComponent(ClanSubCommand command, Client client) {
        Component component = Component.empty().appendNewline();
        NamedTextColor color = (command.requiresServerAdmin() ? NamedTextColor.RED : NamedTextColor.YELLOW);
        if (!command.requiresServerAdmin() || client.hasRank(Rank.ADMIN)) {
            component = component.append(Component.text("/c " + command.getName(), color).append(Component.text(": ", color))
                    .append(Component.text(command.getDescription(), NamedTextColor.GRAY)));
        }
        return component;
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.SUBCOMMAND.name();
        }

        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
