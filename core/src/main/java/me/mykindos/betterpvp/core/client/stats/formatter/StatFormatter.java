package me.mykindos.betterpvp.core.client.stats.formatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;

@CustomLog
@Getter
@Singleton
public class StatFormatter implements IStatFormatter {

    final String statType;

    public StatFormatter(Enum<?> statType) {
        this(statType.name());
    }

    public StatFormatter(String statType) {
        this.statType = statType;
    }
    @Inject
    public StatFormatter() {
        this.statType = "";
    }
}
