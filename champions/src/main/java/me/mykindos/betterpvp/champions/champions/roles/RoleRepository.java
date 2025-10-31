package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHAMPIONS_KILLDEATH_DATA;

@Singleton
public class RoleRepository {

    private final Database database;

    @Inject
    public RoleRepository(Database database) {
        this.database = database;
    }


    public void saveKillDeathData(Role killed, Role killer) {
        String killedRoleName = killed == null ? "NONE" : killed.name();
        String killerRoleName = killer == null ? "NONE" : killer.name();

        String killKey = killerRoleName + "_VS_" + killedRoleName;
        String deathKey = killedRoleName + "_VS_" + killerRoleName;

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            // Insert/update kill data
            ctx.insertInto(CHAMPIONS_KILLDEATH_DATA)
                    .set(CHAMPIONS_KILLDEATH_DATA.MATCHUP, killKey)
                    .set(CHAMPIONS_KILLDEATH_DATA.METRIC, "Kills")
                    .set(CHAMPIONS_KILLDEATH_DATA.VALUE, 1)
                    .onDuplicateKeyUpdate()
                    .set(CHAMPIONS_KILLDEATH_DATA.VALUE, CHAMPIONS_KILLDEATH_DATA.VALUE.plus(1))
                    .execute();

            // Insert/update death data
            ctx.insertInto(CHAMPIONS_KILLDEATH_DATA)
                    .set(CHAMPIONS_KILLDEATH_DATA.MATCHUP, deathKey)
                    .set(CHAMPIONS_KILLDEATH_DATA.METRIC, "Deaths")
                    .set(CHAMPIONS_KILLDEATH_DATA.VALUE, 1)
                    .onDuplicateKeyUpdate()
                    .set(CHAMPIONS_KILLDEATH_DATA.VALUE, CHAMPIONS_KILLDEATH_DATA.VALUE.plus(1))
                    .execute();
        });
    }
}
