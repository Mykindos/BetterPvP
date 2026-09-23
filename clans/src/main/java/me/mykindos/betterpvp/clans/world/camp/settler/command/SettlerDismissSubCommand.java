package me.mykindos.betterpvp.clans.world.camp.settler.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** {@code /settler dismiss <clan> <id>}: sends a settler away, found by the start of its id as the list shows it. */
@Singleton
@SubCommand(SettlerCommand.class)
public class SettlerDismissSubCommand extends Command {

    private final SettlerCommands commands;
    private final SettlerService service;

    @Inject
    public SettlerDismissSubCommand(@NotNull SettlerCommands commands, @NotNull SettlerService service) {
        this.commands = commands;
        this.service = service;
    }

    @Override
    public String getName() {
        return "dismiss";
    }

    @Override
    public String getDescription() {
        return "clans.command.settler.dismiss.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            SettlerCommands.send(player, "clans.command.settler.dismiss.usage");
            return;
        }

        final String prefix = args[1].toLowerCase(Locale.ROOT);
        commands.withCamp(player, args[0], clan -> {
            final SiteKey key = SettlerCommands.key(clan);
            final List<Settler> matches = service.roster(key).orElseGet(Roster::new).getSettlers().stream()
                    .filter(settler -> settler.getId().toString().startsWith(prefix))
                    .toList();
            if (matches.size() != 1) {
                SettlerCommands.send(player, "clans.command.settler.not_found", Component.text(args[1], NamedTextColor.YELLOW));
                return;
            }

            final SettlerResult result = service.dismiss(key, matches.getFirst().getId());
            if (!result.isSuccess()) {
                SettlerCommands.send(player, Objects.requireNonNull(result.getReason()));
                return;
            }
            SettlerCommands.send(player, "clans.command.settler.dismissed", SettlerCommands.name(matches.getFirst()),
                    Component.text(clan.getName(), NamedTextColor.YELLOW));
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? commands.clanNames(args[0]) : List.of();
    }
}
