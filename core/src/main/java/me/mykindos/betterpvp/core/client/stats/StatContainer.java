package me.mykindos.betterpvp.core.client.stats;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.MinecraftStat;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@CustomLog
public class StatContainer implements Unique, IMapListener {
    /**
     * The current period of stat collecting
     */
    public static final String PERIOD = JavaPlugin.getPlugin(Core.class).getConfig().getOrSaveString("stats.period", "test");

    //TODO REFACTOR ALL INSTANCES TO THIS
    public static final String GLOBAL_PERIOD = "";

    private static final ClientManager clientManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientManager.class);

    private final UUID id;

    @Getter
    private final StatConcurrentHashMap stats = new StatConcurrentHashMap();

    public StatContainer(UUID id) {
        this.id = id;
        this.stats.registerListener(this);
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    //todo enum verions
    public Double getAllProperty(String key) {
        return stats.getAll(key);
    }

    public Double getCurrentProperty(String key) {
        return getProperty(PERIOD, key);
    }

    public Double getProperty(String period, String key) {
        return stats.get(period, key);
    }

    public void incrementStat(@Nullable IStat stat, double amount) {
        if (stat == null) {
            log.warn("Attempting to save a null stat").submit();
            return;
        }
        Preconditions.checkArgument(stat.isSavable(), "Stat must be savable to increment");
        incrementStat(stat.getStatName(), amount);
    }

    private void incrementStat(String statName, double amount) {
        log.info("Increment {}", statName).submit();
        this.getStats().increase(StatContainer.PERIOD, statName, amount);
    }

    public Double getCompositeMinecraftStat(MinecraftStat minecraftStat, String period) {
        return stats.getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                    entry.getKey().startsWith(minecraftStat.getBaseStat())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    @Override
    public void onMapValueChanged(String key, Object newValue, @Nullable Object oldValue) {
        new StatPropertyUpdateEvent(this, key, (Double) newValue, (Double) oldValue).callEvent();
        clientManager.saveStatContainerProperty(this, PERIOD, key, (Double) newValue);
    }
}
