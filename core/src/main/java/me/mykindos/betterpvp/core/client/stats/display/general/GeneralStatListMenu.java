package me.mykindos.betterpvp.core.client.stats.display.general;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatPagedMenu;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GeneralStatListMenu extends AbstractStatPagedMenu {
    private static final int numPerItem = 6;
    private List<IStat> stats;
    public GeneralStatListMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, @Nullable Period period, RealmManager realmManager) {
        super(client, previous, type, period, realmManager);
        setItem(8, 5, new DetailedStatsButton());
        stats = client.getStatContainer().getStats().getMyMap().values().stream()
                .flatMap(iStatLongConcurrentMap -> iStatLongConcurrentMap.keySet()
                        .stream())
                .map(IStat::getGenericStat)
                .distinct()
                .sorted(Comparator.comparing(IStat::getQualifiedName))
                .collect(Collectors.toList());
        List<Item> items = new ArrayList<>(stats.size()/numPerItem);
        for (int i = 0; i < stats.size()/numPerItem; i++) {
            items.add(new QualifiedStatListButton(stats, numPerItem, i));
        }

        setContent(items);


    }
}
