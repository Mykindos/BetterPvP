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

import java.util.List;

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


    @Override
    public void execute(Player player, Client client, String... args) {;
        List<ICommand> clanSubCommands = clanCommand.getSubCommands();

        Component component = Component.text("Help:", NamedTextColor.GOLD).appendNewline();

        for (ICommand subCommand : clanSubCommands) {
            if(subCommand instanceof ClanSubCommand clanSubCommand) {
                NamedTextColor color = (clanSubCommand.requiresServerAdmin() ? NamedTextColor.RED : NamedTextColor.GOLD);
                if (!clanSubCommand.requiresServerAdmin() || client.hasRank(Rank.ADMIN)) {
                    component = component.append(Component.text(clanSubCommand.getName(), color).append(Component.text(": ", color))
                            .append(Component.text(clanSubCommand.getDescription(), NamedTextColor.GRAY)).appendNewline());
                }
            }
        }

        UtilMessage.message(player, "Clans", component);

    }

    @Override
    public String getArgumentType(int arg) {
        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
