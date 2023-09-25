package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import javax.naming.Name;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ClanHelpSubCommand extends ClanSubCommand {

    @Inject
    public ClanHelpSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        aliases.addAll(List.of("?", "h"));
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
    public void execute(Player player, Client client, String... args) {

        Component description = Component.text("Description: ", NamedTextColor.GOLD);

        Component usage = Component.text("Usage: ", NamedTextColor.GOLD);

        Component component = Component.text("Help:", NamedTextColor.WHITE).appendNewline()
                .append(Component.text("Clans:", NamedTextColor.YELLOW).appendNewline())
                .append(description).append(Component.text("Base command for clans. Gets your clan information.", NamedTextColor.GRAY)).appendNewline()
                .append(usage).append(Component.text("[subcommand]", NamedTextColor.GRAY)).appendNewline()
                .append(Component.text("Sub Commands:", NamedTextColor.WHITE)).appendNewline()
                .append(Component.text("Info: ", NamedTextColor.YELLOW).appendNewline())
                .append(description).append(Component.text("Get the information of the specified clan.", NamedTextColor.GRAY)).appendNewline()
                .append(usage).append(Component.text("info <clan>", NamedTextColor.GRAY)).appendNewline();

        if (client.hasRank(Rank.ADMIN)) {
            component = component.append(Component.text("Set Dominance:", NamedTextColor.YELLOW)).appendNewline()
                    .append(description).append(Component.text("sets the dominance against the target clan. Dominance must be 0 - 99.", NamedTextColor.GRAY)).appendNewline()
                    .append(usage).append(Component.text("setdominance <clan> <dominance>", NamedTextColor.GRAY));
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
