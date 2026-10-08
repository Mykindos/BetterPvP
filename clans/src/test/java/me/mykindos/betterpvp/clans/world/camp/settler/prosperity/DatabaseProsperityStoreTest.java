package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import me.mykindos.betterpvp.core.database.AsyncDSLContext;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseProsperityStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void ac13_savingNoLongerWritesUpdatedAt() {
        final List<String> statements = new ArrayList<>();
        final DSLContext dsl = DSL.using(new MockConnection(context -> {
            statements.add(context.sql());
            return new MockResult[]{new MockResult(1)};
        }), SQLDialect.POSTGRES);
        final AsyncDSLContext async = mock(AsyncDSLContext.class);
        when(async.executeAsyncVoid(any())).thenAnswer(invocation -> {
            ((Consumer<DSLContext>) invocation.getArgument(0)).accept(dsl);
            return CompletableFuture.completedFuture(null);
        });
        final Database database = mock(Database.class);
        when(database.getAsyncDslContext()).thenReturn(async);

        new DatabaseProsperityStore(database).save(1, 100).join();

        assertTrue(statements.size() == 1 && statements.getFirst().contains("camp_prosperity"), statements::toString);
        assertFalse(statements.getFirst().contains("updated_at"), statements.getFirst());
    }

    @Test
    void ac13_aMigrationDropsUpdatedAt() throws IOException, URISyntaxException {
        final URL migrations = getClass().getClassLoader().getResource("clans-migrations/postgres");
        assertNotNull(migrations);
        try (Stream<Path> files = Files.list(Path.of(migrations.toURI()))) {
            assertTrue(files.anyMatch(DatabaseProsperityStoreTest::dropsUpdatedAt));
        }
    }

    private static boolean dropsUpdatedAt(Path migration) {
        try {
            final String sql = Files.readString(migration).toLowerCase(Locale.ROOT);
            return sql.contains("camp_prosperity") && sql.contains("drop column") && sql.contains("updated_at");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
