package me.mykindos.betterpvp.core.client.stats.period;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.sql.Date;
import java.util.List;

@Singleton
@CustomLog
public class StatPeriodManager extends Manager<StatPeriod> {
    public static final StatPeriod GLOBAL_PERIOD = new StatPeriod("Global", new Date(System.currentTimeMillis()));

    @Getter
    private final StatPeriodRepository repository;

    @Inject
    public StatPeriodManager(StatPeriodRepository repository) {
        this.repository = repository;
        repository.saveCurrentPeriod();
        loadFromList(repository.getAll());
        addObject(StatContainer.GLOBAL_PERIOD_KEY, GLOBAL_PERIOD);
    }

    @Override
    public void loadFromList(List<StatPeriod> objects) {
        objects.forEach(statPeriod -> addObject(statPeriod.getPeriod(), statPeriod));
    }
}
