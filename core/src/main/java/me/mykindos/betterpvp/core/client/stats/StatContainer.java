package me.mykindos.betterpvp.core.client.stats;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletionsConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.events.WrapStatEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.server.Period;
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

    @Getter
    private final Client client;

    @Getter
    private final StatConcurrentHashMap stats = new StatConcurrentHashMap();

    @Getter
    private final AchievementCompletionsConcurrentHashMap achievementCompletions = new AchievementCompletionsConcurrentHashMap();
    @Getter
    private final Set<IStat> changedStats = new HashSet<>();

    public StatContainer(Client client) {
        this.client = client;
        this.stats.registerListener(this);
    }

    @Override
    public UUID getUniqueId() {
        return client.getUniqueId();
    }

    @NotNull
    public Long getProperty(StatFilterType type, @Nullable Period period, IStat stat) {
        return Optional.ofNullable(stats.get(type, period, stat)).orElse(0L);
    }

    public void incrementStat(@Nullable IStat stat, long amount) {
        if (stat == null) {
            log.warn("Attempted to save a null stat").submit();
            return;
        }
        Preconditions.checkArgument(stat.isSavable(), "Stat %s must be savable to increment", stat.getQualifiedName());
        synchronized (this) {
            final WrapStatEvent wrapStatEvent = UtilServer.callEvent(new WrapStatEvent(getUniqueId(), stat));
            final IStat wrappedStat = wrapStatEvent.getStat();
            changedStats.add(wrappedStat);
            this.getStats().increase(Core.getCurrentRealm(), wrappedStat, amount);
        }
    }

    @Override
    public void onMapValueChanged(IStat stat, Long newValue, @Nullable Long oldValue) {
        try {
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                new StatPropertyUpdateEvent(this, stat, newValue, oldValue).callEvent();
            });
        } catch (Exception e) {
            log.error("Exception on map value change {}", stat, e).submit();
        }
    }
}
