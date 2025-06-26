package me.mykindos.betterpvp.core.client.stats.formatter.manager;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.formatter.GenericClientStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.IStatFormatter;
import me.mykindos.betterpvp.core.client.stats.formatter.category.IStatCategory;
import me.mykindos.betterpvp.core.client.stats.formatter.category.SubStatCategory;
import me.mykindos.betterpvp.core.client.stats.impl.ChampionsSkillStat;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.EffectDurationStat;
import me.mykindos.betterpvp.core.client.stats.impl.MinecraftStat;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.model.description.Description;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Getter
@Singleton
public class StatFormatterManager extends Manager<IStatFormatter> {
    private final Map<String, IStatCategory> categories = new ConcurrentHashMap<>();
    /**
     *
     * @param statName
     * @return the {@link KeyValue} of <{@code statName}, {@link IStatFormatter}>
     */
    //todo convert this logic to use optional logic
    public KeyValue<String, IStatFormatter> getStatFormatter(String statName) {
        //special case, this is a Minecraft description
        if (statName.startsWith(MinecraftStat.prefix)) {
            final MinecraftStat minecraftStat = MinecraftStat.fromString(statName);
            IStatFormatter formatter = getObject(minecraftStat.getBaseStat()).orElse(null);
            if (formatter == null) {
                //get the default Minecraft Stat formatter
                formatter = getObject(MinecraftStat.prefix).orElseThrow();
            }
            return new KeyValue<>(statName, formatter);
        }

        if (statName.startsWith(ChampionsSkillStat.PREFIX)) {
            final ChampionsSkillStat championsSkillStat = ChampionsSkillStat.fromString(statName);
            IStatFormatter formatter = getObject(championsSkillStat.getBaseStat()).orElse(null);
            if (formatter == null) {
                //get the default Champions Skill Stat formatter
                formatter = getObject(ChampionsSkillStat.PREFIX).orElseThrow();
            }
            return new KeyValue<>(statName, formatter);
        }

        if (statName.startsWith(EffectDurationStat.PREFIX)) {
            final EffectDurationStat effectDurationStat = EffectDurationStat.fromString(statName);
            IStatFormatter formatter = getObject(effectDurationStat.getStatName()).orElse(null);
            if (formatter == null) {
                //get the default Effect Duration Stat formatter
                formatter = getObject(EffectDurationStat.PREFIX).orElseThrow();
            }
            return new KeyValue<>(statName, formatter);
        }

        IStatFormatter statFormatter = getObject(statName).orElse(null);

        if (statFormatter == null) {
            try {
                final ClientStat clientStat = ClientStat.valueOf(statName);
                return new KeyValue<>(statName, new GenericClientStatFormatter(clientStat));
            } catch (IllegalArgumentException ignored) {

            }
            statFormatter = getObject("").orElseThrow();
        }

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
