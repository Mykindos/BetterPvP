package me.mykindos.betterpvp.clans.clans.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.OldClan;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CustomLog
@Singleton
public class OldClanRepository implements IRepository<OldClan> {
    private final Database database;

    @Inject
    public OldClanRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<OldClan> getAll() {
        List<OldClan> clanList = new ArrayList<>();
        String query = "SELECT * FROM old_clans;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                UUID clanId = UUID.fromString(result.getString(1));
                String name = result.getString(2);
                OldClan oldClan = new OldClan(clanId);
                oldClan.setName(name);
            }
        } catch (SQLException ex) {
            log.error("Failed to load clans", ex);
        }

        return clanList;
    }

    public void save(Clan clan) {
        OldClan oldClan = new OldClan(clan.getId());
        oldClan.setName(clan.getName());
        save(oldClan);
    }

    @Override
    public void save(OldClan oldClan) {
        //todo duplicate names handling
        String saveOldClanQuery = "INSERT INTO old_clans (id, Name) VALUES (?, ?);";
        database.executeUpdate(new Statement(saveOldClanQuery,
                new UuidStatementValue(oldClan.getId()),
                new StringStatementValue(oldClan.getName()))
        );
    }

}
