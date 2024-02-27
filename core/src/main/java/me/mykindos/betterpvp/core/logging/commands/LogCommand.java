package me.mykindos.betterpvp.core.logging.commands;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class LogCommand extends Command {
    @Override
    public String getName() {
        return "log";
    }

    @Override
    public String getDescription() {
        return "Base log command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Log", UtilMessage.deserialize("<green>Usage: /log <legend></green>"));
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND.name();
    }
}
