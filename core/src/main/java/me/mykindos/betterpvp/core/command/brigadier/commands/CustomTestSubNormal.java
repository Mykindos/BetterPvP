package me.mykindos.betterpvp.core.command.brigadier.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.CustomBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;

@Singleton
@CustomLog
@BrigadierSubCommand(CustomTestBase.class)
public class CustomTestSubNormal extends CustomBrigadierCommand {
    @Inject
    public CustomTestSubNormal(ClientManager clientManager) {
        super(clientManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "testnormal";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public BPvPLiteralArgumentBuilder define() {
        BPvPLiteralArgumentBuilder builder = new BPvPLiteralArgumentBuilder(getName())
                .executes(context -> {
                    context.getSource().getSender().sendMessage("Normal Execute");
                    return Command.SINGLE_SUCCESS;
                });
        return builder;
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return true;
    }
}
