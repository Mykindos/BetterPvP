package me.mykindos.betterpvp.core.command.brigadier.scanned;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;

/**
 * {@code /other}, a second top-level command found by scanning this package.
 */
public class ScannedOtherCommand extends BrigadierCommand {

    public ScannedOtherCommand(ClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public String getName() {
        return "other";
    }

    @Override
    public String getDescription() {
        return "Other";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName());
    }
}
