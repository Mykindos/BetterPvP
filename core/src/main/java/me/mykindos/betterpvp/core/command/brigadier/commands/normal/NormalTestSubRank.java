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
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;

@Singleton
@CustomLog
@BrigadierSubCommand(NormalTestBase.class)
public class NormalTestSubRank extends BrigadierCommand {
    @Inject
    public NormalTestSubRank(ClientManager clientManager) {
        super(clientManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "testrank";
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
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(getName())
                .executes(context -> {
                    context.getSource().getSender().sendMessage("Rank Execute: " + getRequiredRank());
                    return Command.SINGLE_SUCCESS;
                }).then(IBrigadierCommand.argument("echo", StringArgumentType.greedyString())
                        .executes(context -> {
                            context.getSource().getSender().sendMessage(context.getArgument("echo", String.class));
                            return Command.SINGLE_SUCCESS;
                        })
                );
        return builder;
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        boolean bool = super.requirement(source);
        log.info("testrank requirement {}", bool).submit();
        return bool;
    }

}
