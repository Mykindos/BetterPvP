package me.mykindos.betterpvp.core.client.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.tables.Realms.REALMS;
import static me.mykindos.betterpvp.core.database.jooq.tables.Seasons.SEASONS;
import static me.mykindos.betterpvp.core.database.jooq.tables.Servers.SERVERS;

@Singleton
public class RealmRepository {
    private final Database database;

    @Inject
    public RealmRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Map<Integer, Realm>> loadAll() {
        return database.getAsyncDslContext().executeAsync(context -> {
            return context.transactionResult(config -> {
                DSLContext ctx = DSL.using(config);
                Map<Integer, Season> seasonMap = new HashMap<>();
                Map<Integer, Server> serverMap = new HashMap<>();
                Map<Integer, Realm> realmMap = new HashMap<>();
                ctx.select(SEASONS.ID, SEASONS.NAME, SEASONS.START)
                        .from(SEASONS)
                        .fetch()
                        .forEach(result -> {
                                    int id = result.get(SEASONS.ID);
                                    String name = result.get(SEASONS.NAME);
                                    LocalDate start = result.get(SEASONS.START);
                                    seasonMap.put(id, new Season(id, name, start));
                                }
                        );
                ctx.select(SERVERS.ID, SERVERS.NAME)
                        .from(SERVERS)
                        .fetch()
                        .forEach(result -> {
                                    int id = result.get(SERVERS.ID);
                                    String name = result.get(SERVERS.NAME);
                                    serverMap.put(id, new Server(id, name));
                                }
                        );
                ctx.select(REALMS.ID, REALMS.SEASON, REALMS.SERVER)
                        .from(REALMS)
                        .fetch()
                        .forEach(result -> {
                                    int id = result.get(REALMS.ID);
                                    int server = result.get(REALMS.SERVER);
                                    int season = result.get(REALMS.SEASON);

                                    realmMap.put(id, new Realm(id, serverMap.get(server), seasonMap.get(season)));
                                }
                        );
                return realmMap;
            });
        });
    }
}
