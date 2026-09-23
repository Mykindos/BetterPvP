package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProsperityStoresTest {

    private final DatabaseProsperityStore database = mock(DatabaseProsperityStore.class);

    private ProsperityStores stores(String configured) {
        final ExtendedYamlConfiguration config = mock(ExtendedYamlConfiguration.class);
        when(config.getOrSaveString(anyString(), anyString())).thenReturn(configured);
        final Clans clans = mock(Clans.class);
        when(clans.getConfig()).thenReturn(config);
        return new ProsperityStores(clans, () -> database);
    }

    @Test
    void theDatabaseIsWhatShips() {
        final ProsperityStores stores = stores(DatabaseProsperityStore.NAME);
        assertSame(database, stores.store());
        assertTrue(stores.names().contains(DatabaseProsperityStore.NAME));
    }

    @Test
    void anotherNetworksStoreIsPluggedInByName() {
        final ProsperityStore theirs = new Theirs();
        final ProsperityStores stores = stores(" Theirs ");
        stores.register("theirs", () -> theirs);
        assertSame(theirs, stores.store());
    }

    @Test
    void anUnknownNameFallsBackToTheDatabase() {
        assertSame(database, stores("nowhere").store());
    }

    private static final class Theirs implements ProsperityStore {

        @Override
        public @NotNull CompletableFuture<Void> save(long clanId, int prosperity) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull CompletableFuture<Void> delete(long clanId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull LinkedHashMap<Long, Integer> top(int limit) {
            return new LinkedHashMap<>();
        }

        @Override
        public @NotNull OptionalInt find(long clanId) {
            return OptionalInt.empty();
        }
    }
}
