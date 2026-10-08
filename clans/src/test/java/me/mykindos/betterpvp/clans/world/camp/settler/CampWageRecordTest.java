package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.storage.SiteStorage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampWageRecordTest {

    private static final long CLAN = 7;

    @Test
    void ac11_wageAndMoraleClocksSurviveACampRoundTrip() {
        final Bytes storage = new Bytes();
        final CampStore writer = new CampStore(storage);
        final Camp camp = writer.load(CLAN).join();
        final Roster roster = camp.getRoster();
        final Settler striker = settler(roster, "builder");
        striker.setAssignment("job");
        striker.changeState(SettlerState.STRIKING, 12_345);
        final Settler unhappy = settler(roster, "farmer");
        unhappy.setUnhappySince(67_890);
        roster.setPayrollAt(99_000);
        roster.setPayrollCarry(0.4);
        writer.changed(CLAN);
        writer.flush().join();

        final Roster read = new CampStore(storage).load(CLAN).join().getRoster();
        assertEquals(99_000, read.getPayrollAt());
        assertEquals(0.4, read.getPayrollCarry(), 1e-9);
        assertEquals(12_345, read.find(striker.getId()).orElseThrow().getStateSince());
        assertEquals(SettlerState.STRIKING, read.find(striker.getId()).orElseThrow().getState());
        assertEquals(67_890, read.find(unhappy.getId()).orElseThrow().getUnhappySince());
    }

    private static Settler settler(Roster roster, String profession) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Hal Two-Coats");
        settler.setProfession(profession);
        settler.setRarity(SettlerRarity.COMMON);
        roster.getSettlers().add(settler);
        return settler;
    }

    /** Keeps records as the bytes {@link SiteStorage#MAPPER} writes. */
    private static final class Bytes implements SiteStorage {

        private final Map<SiteKey, byte[]> records = new HashMap<>();

        @Override
        public @NotNull CompletableFuture<Optional<byte[]>> read(@NotNull SiteKey key) {
            return CompletableFuture.completedFuture(Optional.ofNullable(records.get(key)));
        }

        @Override
        public @NotNull CompletableFuture<Void> write(@NotNull SiteKey key, byte @NotNull [] data) {
            records.put(key, data);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull CompletableFuture<Void> delete(@NotNull SiteKey key) {
            records.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull CompletableFuture<Boolean> exists(@NotNull SiteKey key) {
            return CompletableFuture.completedFuture(records.containsKey(key));
        }
    }
}
