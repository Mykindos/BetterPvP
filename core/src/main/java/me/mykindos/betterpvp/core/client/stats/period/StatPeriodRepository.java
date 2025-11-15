package me.mykindos.betterpvp.core.client.stats.period;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.database.Database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.database.jooq.tables.PeriodMeta.PERIOD_META;

@Singleton
@CustomLog
public class StatPeriodRepository {
    private final Database database;
    @Inject
    public StatPeriodRepository(Database database) {
        this.database = database;
    }

    public List<StatPeriod> getAll() {
        List<StatPeriod> statPeriods = new ArrayList<>();
        try {
            database.getDslContext().select(PERIOD_META.PERIOD, PERIOD_META.START)
                    .from(PERIOD_META)
                    .fetch()
                    .forEach(periodRecord -> {
                        final String period = periodRecord.get(PERIOD_META.PERIOD);
                        final LocalDate date = periodRecord.get(PERIOD_META.START);
                        statPeriods.add(new StatPeriod(period, date));
                    });
        } catch (Exception e) {
            log.error("Error getting periods ", e).submit();
        }
        return statPeriods;
    }

    public void saveCurrentPeriod() {
        final String period = StatContainer.PERIOD_KEY;
        final String insert = "INSERT INTO period_meta (Period) SELECT ? as Period WHERE NOT EXISTS (Select * FROM period_meta WHERE Period = ?)";

        database.getDslContext().insertInto(PERIOD_META)
                .set(PERIOD_META.PERIOD, period)
                .onDuplicateKeyIgnore()
                .execute();

    }

}
