package me.mykindos.betterpvp.core.client.stats;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.events.WrapStatEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@CustomLog
public class StatContainer implements Unique, IStatMapListener {
    /**
     * The current period of stat collecting
     */
    public static final String PERIOD_KEY = JavaPlugin.getPlugin(Core.class).getConfig().getOrSaveString("stats.period", "test");

    public static final String GLOBAL_PERIOD_KEY = "Global";

    private static final ClientManager clientManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientManager.class);

    private final UUID id;

    @Getter
    private final StatConcurrentHashMap stats = new StatConcurrentHashMap();
    @Getter
    private final Set<IStat> changedStats = new HashSet<>();

    public StatContainer(UUID id) {
        this.id = id;
        this.stats.registerListener(this);
    }

    @Override
    public UUID getUniqueId() {
        return id;
    }

    //todo enum verions
    public Double getAllProperty(IStat stat) {
        return stats.getAll(stat);
    }

    public Double getCurrentProperty(IStat stat) {
        return getProperty(PERIOD_KEY, stat);
    }

    @NotNull
    public Double getProperty(String period, IStat stat) {
        return Optional.ofNullable(stats.get(period, stat)).orElse(0d);
    }

    public void incrementStat(@Nullable IStat stat, double amount) {
        if (stat == null) {
            log.warn("Attempted to save a null stat").submit();
            return;
        }
        Preconditions.checkArgument(stat.isSavable(), "Stat must be savable to increment");
        synchronized (this) {
            final WrapStatEvent wrapStatEvent = UtilServer.callEvent(new WrapStatEvent(id, stat));
            final IStat wrappedStat = wrapStatEvent.getStat();
            changedStats.add(wrappedStat);
            this.getStats().increase(StatContainer.PERIOD_KEY, wrappedStat, amount);
        }
    }

    @Override
    public void onMapValueChanged(IStat stat, Double newValue, @Nullable Double oldValue) {
        try {
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                new StatPropertyUpdateEvent(this, stat, newValue, oldValue).callEvent();
            });
        } catch (Exception e) {
            log.error("Exception on map value change {}", stat, e).submit();
        }
    }
}
