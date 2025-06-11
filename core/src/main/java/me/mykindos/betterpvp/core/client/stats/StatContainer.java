package me.mykindos.betterpvp.core.client.stats;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
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
        Double val = stats.get(period, key);
        return val;
    }

    public void incrementStat(IStat stat, double amount) {
        Preconditions.checkArgument(stat.isSavable(), "Stat must be savable to increment");
        incrementStat(stat.getStatName(), amount);
    }

    private void incrementStat(String statName, double amount) {
        this.getStats().increase(StatContainer.PERIOD, statName, amount);
    }

    public Double getCompositeMinecraftStat(MinecraftStat minecraftStat, String period) {
        return stats.getStatsOfPeriod(period).entrySet().stream()
                .filter(entry ->
                    entry.getKey().startsWith(minecraftStat.getBaseStat())
                ).mapToDouble(Map.Entry::getValue)
                .sum();
    }

    public Double getAllCompositeMinecraftStat(MinecraftStat minecraftStat) {
        return getCompositeMinecraftStat(minecraftStat, "");
    }

    @Override
    public void onMapValueChanged(String key, Object newValue, @Nullable Object oldValue) {
        new StatPropertyUpdateEvent(this, key, (Double) newValue, (Double) oldValue).callEvent();
        clientManager.saveStatContainerProperty(this, PERIOD, key, (Double) newValue);
    }
}
