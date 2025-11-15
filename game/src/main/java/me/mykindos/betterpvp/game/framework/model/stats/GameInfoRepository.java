package me.mykindos.betterpvp.game.framework.model.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.database.jooq.tables.GameData.GAME_DATA;
import static me.mykindos.betterpvp.core.database.jooq.tables.GameTeams.GAME_TEAMS;

@Singleton
public class GameInfoRepository implements IRepository<GameInfo> {
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    private final Database database;
    private final ClientManager clientManager;

    @Inject
    public GameInfoRepository(Database database, ClientManager clientManager) {
        this.database = database;
        this.clientManager = clientManager;
    }


    @Override
    public void save(GameInfo object) {
        this.database.getAsyncDslContext().executeAsyncVoid(context -> {
            final List<Query> queries = new ArrayList<>();
            queries.add(
                    context.insertInto(GAME_DATA)
                            .set(GAME_DATA.ID, object.getId())
                            .set(GAME_DATA.GAME, object.getGameName())
                            .set(GAME_DATA.MAP, object.getMapName())
                            .onConflictDoNothing()
            );
            object.getPlayerTeams().forEach((uuid, teamName) -> {
                final long client_id = clientManager.search().offline(uuid).join().orElseThrow().getId();
                queries.add(
                        context.insertInto(GAME_TEAMS)
                                .set(GAME_TEAMS.ID, object.getId())
                                .set(GAME_TEAMS.CLIENT, client_id)
                                .set(GAME_TEAMS.TEAM, teamName)
                                .onDuplicateKeyUpdate()
                                .set(GAME_TEAMS.TEAM, teamName)
                );
            });
            context.transaction(config -> {
                DSLContext ctxl = DSL.using(config);
                ctxl.batch(queries).execute();
            });
        });
    }
}
