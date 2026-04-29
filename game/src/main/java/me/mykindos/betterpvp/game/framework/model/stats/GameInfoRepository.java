package me.mykindos.betterpvp.game.framework.model.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.mykindos.betterpvp.core.database.jooq.tables.GameData.GAME_DATA;
import static me.mykindos.betterpvp.core.database.jooq.tables.GameTeams.GAME_TEAMS;

@Singleton
@CustomLog
public class GameInfoRepository implements IRepository<GameInfo> {

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
                final Optional<Client> clientOpt = clientManager.search().offline(uuid).join();
                if (clientOpt.isEmpty()) {
                    log.warn("Could not resolve client for uuid {} while saving game_teams for game {} — skipping", uuid, object.getId()).submit();
                    return;
                }
                final long clientId = clientOpt.get().getId();
                queries.add(
                        context.insertInto(GAME_TEAMS)
                                .set(GAME_TEAMS.ID, object.getId())
                                .set(GAME_TEAMS.CLIENT, clientId)
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
