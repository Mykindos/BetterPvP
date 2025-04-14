package me.mykindos.betterpvp.core.command.brigadier.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.CustomBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;

@CustomLog
@Singleton
public class CustomTestBase extends CustomBrigadierCommand {
    @Inject
    public CustomTestBase(ClientManager clientManager) {
        super(clientManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "testbase";
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
    public BPvPLiteralArgumentBuilder define() {
        BPvPLiteralArgumentBuilder builder = new BPvPLiteralArgumentBuilder(getName());
        builder.executes(context -> {
            context.getSource().getSender().sendMessage("Base");
            return Command.SINGLE_SUCCESS;
        });
        return builder;
    }
}
