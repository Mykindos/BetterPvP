package me.mykindos.betterpvp.clans.world.camp.settler.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * {@code /settler grant <clan> <rarity> [profession|none] [source] [detail]}: rolls a settler and adds it to a clan's
 * camp. The detail fills in the source's history line, such as the dungeon a prisoner was freed from.
 */
@Singleton
@SubCommand(SettlerCommand.class)
public class SettlerGrantSubCommand extends Command {

    private static final String NONE = "none";
    private static final String DEFAULT_SOURCE = "hiring";

    private final SettlerCommands commands;
    private final SettlerService service;
    private final SettlerGenerator generator;
    private final SettlerConfig config;

    @Inject
    public SettlerGrantSubCommand(@NotNull SettlerCommands commands, @NotNull SettlerService service,
                                  @NotNull SettlerGenerator generator, @NotNull SettlerConfig config) {
        this.commands = commands;
        this.service = service;
        this.generator = generator;
        this.config = config;
    }

    @Override
    public String getName() {
        return "grant";
    }

    @Override
    public String getDescription() {
        return "clans.command.settler.grant.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            SettlerCommands.send(player, "clans.command.settler.grant.usage");
            return;
        }

        final SettlerRarity rarity = Arrays.stream(SettlerRarity.values())
                .filter(value -> value.name().equalsIgnoreCase(args[1]))
                .findFirst()
                .orElse(null);
        if (rarity == null) {
            SettlerCommands.send(player, "clans.command.settler.unknown_rarity", Component.text(args[1], NamedTextColor.YELLOW));
            return;
        }

        final String profession = args.length > 2 && !args[2].equalsIgnoreCase(NONE) ? args[2].toLowerCase(Locale.ROOT) : null;
        if (profession != null && !commands.professionIds().contains(profession)) {
            SettlerCommands.send(player, "clans.command.settler.unknown_profession", Component.text(args[2], NamedTextColor.YELLOW));
            return;
        }

        final SettlerTemplate.SettlerTemplateBuilder template = SettlerTemplate.builder()
                .rarity(rarity)
                .profession(profession)
                .source(args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : DEFAULT_SOURCE);
        if (args.length > 4) {
            template.historyArg(String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
        }

        commands.withCamp(player, args[0], clan -> {
            final Settler settler = generator.roll(template.build(), config.getTable(), ThreadLocalRandom.current());
            final SettlerResult result = service.grant(SettlerCommands.key(clan), settler);
            if (!result.isSuccess()) {
                SettlerCommands.send(player, Objects.requireNonNull(result.getReason()));
                return;
            }
            SettlerCommands.send(player, "clans.command.settler.granted", SettlerCommands.name(settler),
                    Component.text(clan.getName(), NamedTextColor.YELLOW));
            SettlerCommands.send(player, commands.line(settler));
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        final List<String> completions = new ArrayList<>();
        final String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        final Stream<String> options = switch (args.length) {
            case 1 -> commands.clanNames(typed).stream();
            case 2 -> Arrays.stream(SettlerRarity.values()).map(rarity -> rarity.name().toLowerCase(Locale.ROOT));
            case 3 -> Stream.concat(commands.professionIds().stream(), Stream.of(NONE));
            case 4 -> config.getTable().getHistories().keySet().stream();
            default -> Stream.empty();
        };
        options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).forEach(completions::add);
        return completions;
    }
}
