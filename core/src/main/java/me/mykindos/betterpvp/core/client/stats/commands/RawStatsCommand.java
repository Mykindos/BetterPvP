package me.mykindos.betterpvp.core.client.stats.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.formatter.manager.StatFormatterManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.model.IStringName;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class RawStatsCommand extends Command implements IStringName {
    private final StatFormatterManager statFormatterManager;

    @Inject
    public RawStatsCommand(StatFormatterManager statFormatterManager) {
        this.statFormatterManager = statFormatterManager;
    }

    @Override
    public String getName() {
        return "rawstats";
    }

    @Override
    public String getDescription() {
        return "Show raw stats for yourself";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final String period = args.length > 0 ? args[0] : StatContainer.GLOBAL_PERIOD_KEY;
        //TODO move this logic to a menu
        final StatContainer container = client.getStatContainer();

        final List<Item> statItems = container.getStats().getStatsOfPeriod(period).entrySet().stream()
                .map(entry -> {
                    final String statName = entry.getKey();
                    return statFormatterManager.getStatFormatter(statName).getValue().getDescription(statName, container, period);
                })
                .map(Description::toSimpleItem)
                .map(Item.class::cast)
                .toList();
        new ViewCollectionMenu(player.getName() + "'s Stats", statItems, null).show(player);
    }

    //todo type hints for period
}
