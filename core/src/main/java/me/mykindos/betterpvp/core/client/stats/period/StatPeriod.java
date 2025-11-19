package me.mykindos.betterpvp.core.client.stats.period;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StatPeriod implements Comparable<StatPeriod> {
    @Getter
    @EqualsAndHashCode.Include
    @NotNull
    private final String period;
    @NotNull
    private final LocalDate date;

    public StatPeriod(@NotNull String period, @NotNull LocalDate date) {
        this.period = period;
        this.date = date;
    }

    @Override
    public int compareTo(@NotNull StatPeriod o) {
        return -Integer.signum(date.compareTo(o.date));
    }

    @Override
    public String toString() {
        return "StatPeriod{" +
                "period='" + period + '\'' +
                '}';
    }
}
