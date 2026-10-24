package me.mykindos.betterpvp.core.command.brigadier.scanned;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierNodes;

/**
 * {@code /scanned child}, found by scanning this package.
 */
@BrigadierSubCommand(ScannedRootCommand.class)
public class ScannedChildCommand extends BrigadierCommand {

    public ScannedChildCommand(ClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public String getName() {
        return "child";
    }

    @Override
    public String getDescription() {
        return "Child";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return BrigadierNodes.literal(getName());
    }
}
