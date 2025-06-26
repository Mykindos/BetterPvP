package me.mykindos.betterpvp.core.client.stats.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.StatMenu;
import me.mykindos.betterpvp.core.client.stats.formatter.manager.StatFormatterManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.model.IStringName;
import org.bukkit.entity.Player;

@Singleton
public class StatsCommand extends Command implements IStringName {
    private final StatFormatterManager statFormatterManager;

    @Inject
    public StatsCommand(StatFormatterManager statFormatterManager) {
        this.statFormatterManager = statFormatterManager;
    }

    @Override
    public String getName() {
        return "statsmenu";
    }

    @Override
    public String getDescription() {
        return "Show stats for yourself";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String period = args.length > 0 ? args[0] : StatContainer.GLOBAL_PERIOD;
        new StatMenu(client, null, period, statFormatterManager, null).show(player);
    }

    //todo type hints for period
}
