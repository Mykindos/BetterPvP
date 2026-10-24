package me.mykindos.betterpvp.core.command.brigadier.scanned;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;

/**
 * {@code /scanned}, found by scanning this package.
 */
public class ScannedRootCommand extends BrigadierCommand {

    public ScannedRootCommand(ClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public String getName() {
        return "scanned";
    }

    @Override
    public String getDescription() {
        return "Scanned";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName());
    }
}
