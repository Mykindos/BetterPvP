package me.mykindos.betterpvp.core.client.stats.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
@Getter
@Singleton
public class StatFormatter implements IStatFormatter {

    @Nullable
    private final IStat stat;

    public StatFormatter(@NotNull IStat stat) {
        this.stat = stat;
    }

    @Inject
    public StatFormatter() {
        this.stat = null;
    }

    @Override
    public @Nullable String getCategory() {
        return null;
    }

    @Override
    public String getStatType() {
        return stat == null ? "" : stat.getStatName();
    }

    @Override
    public @Nullable IStat getStat() {
        return stat;
    }
}
