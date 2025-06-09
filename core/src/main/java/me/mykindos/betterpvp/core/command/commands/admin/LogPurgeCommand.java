package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class LogPurgeCommand extends Command {

    private final LogRepository logRepository;

    @Inject
    public LogPurgeCommand(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public String getName() {
        return "logpurge";
    }

    @Override
    public String getDescription() {
        return "Deletes all logs that don't have an associated action";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if(args.length == 0) {
            UtilMessage.simpleMessage(player, "Logs", "Correct Usage: /logpurge <days>");
            return;
        }

        int days = Integer.parseInt(args[0]);
        logRepository.purgeLogs(days);

    }


    @Override
    public Rank getRequiredRank() {
        return Rank.DEVELOPER;
    }

    @Override
    public String getArgumentType(int argCount) {

        return ArgumentType.NONE.name();
    }

}
