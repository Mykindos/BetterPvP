package me.mykindos.betterpvp.core.client.stats.commands;

import java.util.Arrays;
import java.util.List;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.ClientStat;
import me.mykindos.betterpvp.core.client.stats.display.IStatItemView;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.model.IStringName;
import org.bukkit.entity.Player;

public class RawStatsCommand extends Command implements IStringName {
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
        final String period = args.length > 1 ? args[0] : "";
        final List<Item> statItems = Arrays.stream(ClientStat.values())
                .map(stat -> (Item) new SimpleItem(IStatItemView.generalStat.getItemView(player, stat, client.getStatContainer(), period)))
                .toList();
        new ViewCollectionMenu(player.getName() + "'s Stats", statItems, null).show(player);
    }

    //todo type hints for period
}
