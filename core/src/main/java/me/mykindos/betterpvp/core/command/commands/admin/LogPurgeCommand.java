package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
public class LogPurgeCommand extends Command {

    private final Core core;
    private final Database database;

    @Inject
    public LogPurgeCommand(Core core, Database database) {
        this.core = core;
        this.database = database;
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
        long daysToMillis = days * (24L * 60L * 60L * 1000L);

        UtilServer.runTaskAsync(core, () -> {
            UtilMessage.simpleMessage(player, "Logs", "Purging logs older than <green>%d<reset> days.", days);
            database.executeUpdate(new Statement("DELETE FROM logs WHERE action is NULL AND Time < ?",
                    new LongStatementValue(System.currentTimeMillis() - daysToMillis)));
        });

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
