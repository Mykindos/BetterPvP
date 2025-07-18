package me.mykindos.betterpvp.core.client.stats.period;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;

import javax.sql.rowset.CachedRowSet;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        final Statement statement = Statement.builder()
                .select("period_meta", "Period", "Start")
                .build();
        try (CachedRowSet rowSet = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
            while (rowSet.next()) {
                String period = rowSet.getString("Period");
                Date date = rowSet.getDate("Start");
                statPeriods.add(new StatPeriod(period, date));
            }
        } catch (SQLException e) {
            log.error("Error getting periods ", e).submit();
        }
        return statPeriods;
    }

    public void saveCurrentPeriod() {
        final String period = StatContainer.PERIOD;
        final String insert = "INSERT INTO period_meta (Period) SELECT ? as Period WHERE NOT EXISTS (Select * FROM period_meta WHERE Period = ?)";
        final Statement statement = new Statement(insert,
                new StringStatementValue(period),
                new StringStatementValue(period));
        database.executeUpdate(statement, TargetDatabase.GLOBAL);
    }

}
