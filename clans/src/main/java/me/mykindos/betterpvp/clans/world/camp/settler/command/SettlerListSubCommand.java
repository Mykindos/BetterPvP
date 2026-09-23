package me.mykindos.betterpvp.clans.world.camp.settler.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** {@code /settler list <clan>}: every settler in a clan's camp, and how many more it has room for. */
@Singleton
@SubCommand(SettlerCommand.class)
public class SettlerListSubCommand extends Command {

    private final SettlerCommands commands;
    private final SettlerService service;

    @Inject
    public SettlerListSubCommand(@NotNull SettlerCommands commands, @NotNull SettlerService service) {
        this.commands = commands;
        this.service = service;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "clans.command.settler.list.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            SettlerCommands.send(player, "clans.command.settler.list.usage");
            return;
        }

        commands.withCamp(player, args[0], clan -> {
            final SiteKey key = SettlerCommands.key(clan);
            final Roster roster = service.roster(key).orElseGet(Roster::new);
            SettlerCommands.send(player, "clans.command.settler.list.header",
                    Component.text(clan.getName(), NamedTextColor.YELLOW),
                    Component.text(roster.size(), NamedTextColor.GREEN),
                    Component.text(service.populationCap(key), NamedTextColor.GREEN));
            if (roster.size() == 0) {
                SettlerCommands.send(player, "clans.command.settler.list.empty");
            }
            roster.getSettlers().forEach(settler -> SettlerCommands.send(player, commands.line(settler)));
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? commands.clanNames(args[0]) : List.of();
    }
}
