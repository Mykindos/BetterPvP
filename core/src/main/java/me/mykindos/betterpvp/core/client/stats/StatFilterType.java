package me.mykindos.betterpvp.core.client.stats;

import lombok.Getter;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.jetbrains.annotations.Nullable;

public enum  StatFilterType {
    REALM(Realm.class),
    SEASON(Season.class),
    ALL(null);

    @Getter
    private final Class<? extends Period> type;

    StatFilterType(Class<? extends Period> type) {
        this.type = type;
    }

    public boolean filter(@Nullable Period period, Realm key) {
        if (this != ALL && !type.isInstance(period)) {
            throw new IllegalArgumentException("Period must be of type " + type.getName() + " but instead found " + (period == null ? "null" : period.getClass()));
        }
        return switch (this) {
            case REALM -> key.equals(period);
            case SEASON -> key.getSeason().equals(period);
            case ALL -> true;
        };
    }
}
