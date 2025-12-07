package me.mykindos.betterpvp.core.client.stats.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.start.StartStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.model.IStringName;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
public class TestStatsCommand extends Command implements IStringName {
    private final StatPeriodManager statPeriodManager;

    @Inject
    public TestStatsCommand(StatPeriodManager statPeriodManager) {
        this.statPeriodManager = statPeriodManager;
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
        final String periodKey = args.length > 0 ? args[0] : StatContainer.GLOBAL_PERIOD_KEY;
        new StartStatMenu(client, null, periodKey, statPeriodManager).show(player);
        client.getStatContainer().getStats().iterator().forEachRemaining(statData -> {
            log.info(statData.toString()).submit();
        });
    }
}
