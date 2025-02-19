package me.mykindos.betterpvp.core.command.brigadier.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
public class Test extends BrigadierCommand {

    @Inject
    public Test(ClientManager clientManager) {
        super(clientManager);
    }


    @Override
    public String getName() {
        return "testcommand";
    }

    @Override
    public String getDescription() {
        return "a command to test brigadier implementations and show examples of how to use it";
    }

    /**
     * Define the command, using normal rank based permissions
     *
     * @return the builder for the command
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return Commands.literal("testcommand")
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player player) {
                        UtilMessage.message(player, "test command");
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("number", IntegerArgumentType.integer())
                        .executes(context -> {
                            final int number = context.getArgument("number", int.class);
                            if (context.getSource().getExecutor() instanceof Player player) {
                                UtilMessage.message(player, "testcommand", "The number entered was %s", number);
                            }
                            return Command.SINGLE_SUCCESS;
                        }).requires(source -> {
                            return executorIsPlayer(source) && source.getExecutor().isInWater();
                        })
                ).then(Commands.literal("offlinename")
                        //must be before selector if also using a player selector (Allowing you to combine both)
                        .then(Commands.argument("Offline Client", BPvPArgumentTypes.PlayerName)
                                .executes(context -> {
                                    final String targetName = context.getArgument("Offline Clan Member", String.class);
                                    final CommandSender sender = context.getSource().getSender();
                                    getOfflineClientByName(targetName, sender).thenAccept(clientOptional -> {
                                        if (clientOptional.isEmpty()) return;
                                        final Client targetClient = clientOptional.get();
                                        context.getSource().getExecutor().sendMessage(targetClient.getRank().toString());
                                        });
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                ).then(Commands.literal("onlineplayer")
                        .then(Commands.argument("Online Player", ArgumentTypes.player())
                                .executes(context -> {
                                    final Player target = context.getArgument("Online Player", PlayerSelectorArgumentResolver.class)
                                            .resolve(context.getSource()).getFirst();

                                    context.getSource().getExecutor().sendMessage("Found player " + target.getName());
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                ;
    }
}
