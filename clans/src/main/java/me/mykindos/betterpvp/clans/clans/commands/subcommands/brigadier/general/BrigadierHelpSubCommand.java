package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierHelpSubCommand extends ClanBrigadierCommand {
    private static final int NUMPERPAGE = 5;


    @Inject
    protected BrigadierHelpSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "help";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Get information about clans s";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .executes(context -> {
                    doHelp(context, "", 1);
                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("page", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            int page = context.getArgument("page", Integer.class);
                            doHelp(context, "", page);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(IBrigadierCommand.argument("filter", StringArgumentType.word())
                        .executes(context -> {
                            String filter = context.getArgument("filter", String.class);
                            doHelp(context, filter, 1);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(IBrigadierCommand.argument("page", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    String filter = context.getArgument("filter", String.class);
                                    int page = context.getArgument("page", Integer.class);
                                    doHelp(context, filter, page);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private void doHelp(CommandContext<CommandSourceStack> context, String filter, int page) throws CommandSyntaxException {
        final Player player = getPlayerFromExecutor(context);
        final Client client = getClientFromExecutor(context);


        final IBrigadierCommand parent = getParent();
        List<IBrigadierCommand> childCommands = Objects.requireNonNull(parent).getChildren().stream()
                .filter(child -> child.getName().toLowerCase().contains(filter.toLowerCase()))
                .filter(child -> client.hasRank(child.getRequiredRank()))
                .sorted(Comparator.comparing(IBrigadierCommand::getName))
                .toList();


        int count = 0;
        final int start = (page - 1) * NUMPERPAGE;
        int end = start + NUMPERPAGE;
        final int size = childCommands.size();
        int totalPages = size / NUMPERPAGE;
        if (size % NUMPERPAGE > 0) {
            totalPages++;
        }
        Component component = UtilMessage.deserialize("<yellow>Help<gray>: <white>" + page + "<gray> / <white>" + totalPages);

        if (start <= size) {
            if (end > size) end = size;
            for (IBrigadierCommand subCommand : childCommands.subList(start, end)) {
                if (count == NUMPERPAGE) break;
                component = component.append(addHelpCommandComponent(context, subCommand));

                count++;
            }
        }
        UtilMessage.message(player, "Clans", component);

    }

    private Component addHelpCommandComponent(CommandContext<CommandSourceStack> context, IBrigadierCommand command) {
        NamedTextColor color = command.requirement(context.getSource()) ? NamedTextColor.GREEN : NamedTextColor.RED;
        Component commandComponent = Component.empty();
        commandComponent = commandComponent.appendNewline()
                .append(Component.text(command.getName(), color)
                        .hoverEvent(HoverEvent.showText(command.getRequirementComponent(context)))
                        .clickEvent(ClickEvent.suggestCommand("/" + command.getName()))
                ).appendSpace()
                .append(Component.text(command.getDescription(), NamedTextColor.WHITE)).appendNewline()
                .append(Component.text("Usage:", NamedTextColor.GOLD)).appendSpace()
                //todo make this actually be useful usage text, might have to goto all end nodes
                .append(Component.text(command.build().getUsageText(), NamedTextColor.WHITE));
        return commandComponent;
    }
}
