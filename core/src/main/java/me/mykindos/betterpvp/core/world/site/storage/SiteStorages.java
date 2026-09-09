package me.mykindos.betterpvp.core.world.site.storage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The named places a site record can be kept, and which one this server uses.
 * <p>
 * Local files are what ships. Somewhere shared between machines is registered the same way and named in config, with
 * no edit to anything that reads or writes a record:
 *
 * <pre>{@code
 * storages.register("s3", () -> new S3SiteStorage(...));
 * }</pre>
 *
 * Register during startup, before anything asks for a record. What the seam resolves to is read once and kept.
 */
@Singleton
@CustomLog
public class SiteStorages {

    private final Map<String, Supplier<SiteStorage>> stores = new ConcurrentHashMap<>();
    private final Core core;

    private volatile SiteStorage resolved;

    @Inject
    public SiteStorages(@NotNull Core core, @NotNull Provider<LocalSiteStorage> local) {
        this.core = core;
        register(LocalSiteStorage.NAME, local::get);
    }

    public void register(@NotNull String name, @NotNull Supplier<SiteStorage> factory) {
        if (stores.put(key(name), factory) != null) {
            log.warn("A second site storage named '{}' replaced the first", key(name)).submit();
        }
    }

    public @NotNull Set<String> names() {
        return Set.copyOf(stores.keySet());
    }

    /** What this server keeps records in, chosen once from {@code core.site.storage}. */
    public @NotNull SiteStorage storage() {
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

    private @NotNull SiteStorage choose() {
        final String named = core.getConfig().getOrSaveString("core.site.storage", LocalSiteStorage.NAME);
        final Optional<SiteStorage> chosen = Optional.ofNullable(stores.get(key(named))).map(Supplier::get);

        if (chosen.isPresent()) {
            log.info("Keeping site records in '{}' storage", key(named)).submit();
            return chosen.get();
        }

        // Said out loud, because falling back quietly is how a season's records end up on the wrong disk.
        log.warn("No site storage named '{}' could be used, keeping records locally", named).submit();
        return stores.get(LocalSiteStorage.NAME).get();
    }

    private @NotNull String key(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
