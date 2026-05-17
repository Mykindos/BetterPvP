package me.mykindos.betterpvp.progression.booster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_BOOSTERS;

@Singleton
@CustomLog
public class BoosterRepository {

    private final Database database;
    private final ClientManager clientManager;

    @Inject
    public BoosterRepository(Database database, ClientManager clientManager) {
        this.database = database;
        this.clientManager = clientManager;
    }

    public CompletableFuture<Optional<Long>> getBoosterExpiry(UUID uuid) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            Long clientId = ctx.select(CLIENTS.ID)
                    .from(CLIENTS)
                    .where(CLIENTS.UUID.eq(uuid.toString()))
                    .fetchOne(CLIENTS.ID);
            
            if (clientId == null) return Optional.empty();

            Long expiry = ctx.select(PROGRESSION_BOOSTERS.EXPIRY)
                    .from(PROGRESSION_BOOSTERS)
                    .where(PROGRESSION_BOOSTERS.CLIENT.eq(clientId.intValue()))
                    .fetchOne(PROGRESSION_BOOSTERS.EXPIRY);

            return Optional.ofNullable(expiry);
        });
    }

    public void saveBooster(UUID uuid, long expiry) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Long clientId = ctx.select(CLIENTS.ID)
                    .from(CLIENTS)
                    .where(CLIENTS.UUID.eq(uuid.toString()))
                    .fetchOne(CLIENTS.ID);

            if (clientId == null) return;

            ctx.insertInto(PROGRESSION_BOOSTERS)
                    .set(PROGRESSION_BOOSTERS.CLIENT, clientId.intValue())
                    .set(PROGRESSION_BOOSTERS.EXPIRY, expiry)
                    .onConflict(PROGRESSION_BOOSTERS.CLIENT)
                    .doUpdate()
                    .set(PROGRESSION_BOOSTERS.EXPIRY, expiry)
                    .execute();
        });
    }
}
