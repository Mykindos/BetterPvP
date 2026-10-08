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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void ac13_aMigrationLetsUpdatedAtBeEmpty() throws IOException, URISyntaxException {
        final URL migrations = getClass().getClassLoader().getResource("clans-migrations/postgres");
        assertNotNull(migrations);
        try (Stream<Path> files = Files.list(Path.of(migrations.toURI()))) {
            assertTrue(files.anyMatch(DatabaseProsperityStoreTest::relaxesUpdatedAt));
        }
    }

    @Test
    void ac10_topReadsTheMostProsperousCampsFirst() {
        final List<String> statements = new ArrayList<>();
        final List<Object[]> bindings = new ArrayList<>();
        final DSLContext dsl = DSL.using(new MockConnection(context -> {
            statements.add(context.sql());
            bindings.add(context.bindings());
            return new MockResult[]{new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult())};
        }), SQLDialect.POSTGRES);
        final Database database = mock(Database.class);
        when(database.getDslContext()).thenReturn(dsl);

        new DatabaseProsperityStore(database).top(10);

        assertEquals(1, statements.size(), statements::toString);
        final String sql = statements.getFirst().toLowerCase(Locale.ROOT);
        assertTrue(sql.endsWith("order by \"prosperity\" desc fetch next ? rows only"), sql);
        assertEquals(1, bindings.getFirst().length);
        assertEquals(10L, ((Number) bindings.getFirst()[0]).longValue());
    }

    @Test
    void ac13_theUpdatedAtMigrationIsAFlywayMigrationAfterTheProsperityTable() throws IOException, URISyntaxException {
        final Pattern flyway = Pattern.compile("V(\\d+(?:_\\d+)*)__\\w+\\.sql");
        final URL migrations = getClass().getClassLoader().getResource("clans-migrations/postgres");
        assertNotNull(migrations);
        final List<Path> files;
        try (Stream<Path> listed = Files.list(Path.of(migrations.toURI()))) {
            files = listed.toList();
        }
        final Optional<Path> drop = files.stream().filter(DatabaseProsperityStoreTest::relaxesUpdatedAt).findFirst();
        assertTrue(drop.isPresent());
        final Matcher dropName = flyway.matcher(drop.get().getFileName().toString());
        assertTrue(dropName.matches(), drop.get().getFileName().toString());

        for (Path other : files) {
            if (other.equals(drop.get()) || !readLower(other).contains("camp_prosperity")) {
                continue;
            }
            final Matcher otherName = flyway.matcher(other.getFileName().toString());
            assertTrue(otherName.matches(), other.getFileName().toString());
            assertTrue(compareVersions(dropName.group(1), otherName.group(1)) > 0,
                    drop.get().getFileName() + " must run after " + other.getFileName());
        }
    }

    private static int compareVersions(String left, String right) {
        final String[] a = left.split("_");
        final String[] b = right.split("_");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            final long x = i < a.length ? Long.parseLong(a[i]) : 0;
            final long y = i < b.length ? Long.parseLong(b[i]) : 0;
            if (x != y) {
                return Long.compare(x, y);
            }
        }
        return 0;
    }

    private static String readLower(Path migration) {
        try {
            return Files.readString(migration).toLowerCase(Locale.ROOT);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean relaxesUpdatedAt(Path migration) {
        try {
            final String sql = Files.readString(migration).toLowerCase(Locale.ROOT);
            return sql.contains("camp_prosperity") && sql.contains("updated_at") && sql.contains("drop not null");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
