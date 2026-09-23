package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The named places Prosperity can be kept, and which one this server uses.
 * <p>
 * The database is what ships. Another network's storage is registered the same way and named in config, with no edit
 * to anything that reads or writes Prosperity:
 *
 * <pre>{@code
 * stores.register("theirs", () -> new TheirProsperityStore(...));
 * }</pre>
 *
 * Register during startup, before anything asks for the store. What {@code clans.camp.prosperity.store} names is read
 * once and kept.
 */
@Singleton
@CustomLog
public class ProsperityStores {

    private final Map<String, Supplier<ProsperityStore>> stores = new ConcurrentHashMap<>();
    private final Clans clans;

    private volatile ProsperityStore resolved;

    @Inject
    public ProsperityStores(@NotNull Clans clans, @NotNull Provider<DatabaseProsperityStore> database) {
        this.clans = clans;
        register(DatabaseProsperityStore.NAME, database::get);
    }

    public void register(@NotNull String name, @NotNull Supplier<ProsperityStore> factory) {
        if (stores.put(key(name), factory) != null) {
            log.warn("A second Prosperity store named '{}' replaced the first", key(name)).submit();
        }
    }

    public @NotNull Set<String> names() {
        return Set.copyOf(stores.keySet());
    }

    /** Where this server keeps Prosperity, chosen once from {@code clans.camp.prosperity.store}. */
    public @NotNull ProsperityStore store() {
        if (resolved == null) {
            synchronized (this) {
                if (resolved == null) {
                    resolved = choose();
                }
            }
        }
        return resolved;
    }

    public void close() {
        if (resolved != null) {
            resolved.close();
        }
    }

    private @NotNull ProsperityStore choose() {
        final String named = clans.getConfig().getOrSaveString("clans.camp.prosperity.store", DatabaseProsperityStore.NAME);
        final Optional<ProsperityStore> chosen = Optional.ofNullable(stores.get(key(named))).map(Supplier::get);
        if (chosen.isPresent()) {
            log.info("Keeping camp Prosperity in '{}' storage", key(named)).submit();
            return chosen.get();
        }
        log.warn("No Prosperity store named '{}' could be used, keeping it in the database", named).submit();
        return stores.get(DatabaseProsperityStore.NAME).get();
    }

    private static @NotNull String key(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
