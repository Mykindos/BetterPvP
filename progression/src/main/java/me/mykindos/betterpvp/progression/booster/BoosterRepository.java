package me.mykindos.betterpvp.progression.booster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;

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

            Long expiry = ctx.select(DSL.field("expiry", Long.class))
                    .from(DSL.table("progression_boosters"))
                    .where(DSL.field("client").eq(clientId))
                    .fetchOne(DSL.field("expiry", Long.class));

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

            ctx.insertInto(DSL.table("progression_boosters"))
                    .set(DSL.field("client"), clientId.intValue())
                    .set(DSL.field("expiry"), expiry)
                    .onConflict(DSL.field("client"))
                    .doUpdate()
                    .set(DSL.field("expiry"), expiry)
                    .execute();
        });
    }
}
