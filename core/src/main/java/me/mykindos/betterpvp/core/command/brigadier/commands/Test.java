package me.mykindos.betterpvp.core.command.brigadier.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class Test extends BrigadierCommand {

    @Inject
    public Test(ClientManager clientManager) {
        super(clientManager);
    }


    @Override
    public String getName() {
        return "brigadiersearch";
    }

    @Override
    public String getDescription() {
        return "a command to test brigadier implementations";
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
                            int number = context.getArgument("number", int.class);
                            if (context.getSource().getExecutor() instanceof Player player) {
                                UtilMessage.message(player, "testcommand", "The number entered was %s", number);
                            }
                            return Command.SINGLE_SUCCESS;
                        }).requires(source -> {
                            return executorIsPlayer(source) && source.getExecutor().isInWater();
                        })
                );
    }
}
