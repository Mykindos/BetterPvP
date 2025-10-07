package me.mykindos.betterpvp.core.client.stats.formatter.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatBuilder;
import me.mykindos.betterpvp.core.client.stats.formatter.GenericClientStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.IStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.category.IStatCategory;
import me.mykindos.betterpvp.core.client.stats.formatter.category.SubStatCategory;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IBuildableStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.model.description.Description;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Getter
@Singleton
@CustomLog
public class StatFormatterManager extends Manager<IStatFormatter> {
    private final Map<String, IStatCategory> categories = new ConcurrentHashMap<>();
    private final StatBuilder statBuilder;

    @Inject
    public StatFormatterManager(StatBuilder statBuilder) {
        this.statBuilder = statBuilder;
    }

    /**
     *
     * @param statName
     * @return the {@link KeyValue} of <{@code statName}, {@link IStatFormatter}>
     */
    //todo convert this logic to use optional logic
    public KeyValue<String, IStatFormatter> getStatFormatter(String statName) {
        final IStat stat = statBuilder.getStatForStatName(statName);

        IStatFormatter alternate = null;

        for (IStatFormatter formatter : getObjects().values()) {
            //todo save prefix one here (?)
            // or make it a multimap allowing null keys
            if (stat.equals(formatter.getStat())) {
                return new KeyValue<>(statName, formatter);
            }
            if (formatter.getStat() != null && formatter.getStat().containsStat(statName)) {
                alternate = formatter;
            }
        }

        if (alternate != null) {
            return new KeyValue<>(statName, alternate);
        }

        if (stat instanceof IBuildableStat buildableStat) {
            final Optional<IStatFormatter> buildableFormatter = getObject(buildableStat.getPrefix());
            if (buildableFormatter.isPresent()) {
                return new KeyValue<>(statName, buildableFormatter.get());
            }
        }

        try {
            final ClientStat clientStat = ClientStat.valueOf(statName);
            return new KeyValue<>(statName, new GenericClientStatFormatter(clientStat));
        } catch (IllegalArgumentException ignored) {

        }
        final IStatFormatter statFormatter = getObject("").orElseThrow();
        return new KeyValue<>(statName, statFormatter);

    }

    public Description getCategoryDescription(String categoryName) {
        return categories.get(categoryName).getDescription();
    }

    public Collection<IStatCategory> getRootCategories() {
        return categories.values().stream()
                .filter(iStatCategory -> !iStatCategory.getClass().isAnnotationPresent(SubStatCategory.class))
                .collect(Collectors.toSet());
    }


    //todo get all non default formatter keys (for retrieval from db so we can get 0s)

    //is currently done in an event, should also prob store those instead of quering every open
}
