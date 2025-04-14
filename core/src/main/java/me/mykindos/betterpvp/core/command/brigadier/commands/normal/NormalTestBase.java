package me.mykindos.betterpvp.core.command.brigadier.commands.normal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;

@CustomLog
@Singleton
public class NormalTestBase extends BrigadierCommand {
    @Inject
    public NormalTestBase(ClientManager clientManager) {
        super(clientManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "normaltestbase";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Base Command for testing purposes";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(getName());
        builder.executes(context -> {
            context.getSource().getSender().sendMessage("Base");
            return Command.SINGLE_SUCCESS;
        }).then(IBrigadierCommand.argument("echo", StringArgumentType.greedyString())
                .executes(context -> {
                    context.getSource().getSender().sendMessage(context.getArgument("echo", String.class));
                    return Command.SINGLE_SUCCESS;
                })
        );
        return builder;
    }
}
