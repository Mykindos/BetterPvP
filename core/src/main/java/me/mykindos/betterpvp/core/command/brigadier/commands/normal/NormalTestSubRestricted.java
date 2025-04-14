package me.mykindos.betterpvp.core.command.brigadier.commands.normal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;

@Singleton
@CustomLog
@BrigadierSubCommand(NormalTestBase.class)
public class NormalTestSubRestricted extends BrigadierCommand {
    @Inject
    public NormalTestSubRestricted(ClientManager clientManager) {
        super(clientManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "testrestricted";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "test restriction";
    }

    @Override
    public BPvPLiteralArgumentBuilder define() {
        BPvPLiteralArgumentBuilder builder = new BPvPLiteralArgumentBuilder(getName())
                .executes(context -> {
                    context.getSource().getSender().sendMessage("Restricted Execute");
                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("echo", StringArgumentType.greedyString())
                        .executes(context -> {
                            context.getSource().getSender().sendMessage(context.getArgument("echo", String.class));
                            return Command.SINGLE_SUCCESS;
                        })
                );
        return builder;
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return false;
    }
}
