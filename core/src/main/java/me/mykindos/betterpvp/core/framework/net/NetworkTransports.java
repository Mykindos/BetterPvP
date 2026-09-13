package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The named implementations of each network seam, and where another network adds its own.
 * <p>
 * The three seams are registered separately on purpose. A network can carry messages one way, hold shared state
 * another and move players a third, and bundling them would force whoever integrates to supply pieces they do not
 * have. Joining a network that already has a message bus but uses the ordinary proxy transfer should mean replacing
 * one of these and leaving the others alone.
 * <p>
 * Register during startup, before anything asks for a seam. What each seam resolves to is read once and kept.
 */
@Singleton
@CustomLog
public class NetworkTransports {

    /** The name meaning "work it out", which is what ships. */
    public static final String AUTO = "auto";

    private final Map<String, Supplier<MessageBus>> buses = new ConcurrentHashMap<>();
    private final Map<String, Supplier<SiteDirectory>> directories = new ConcurrentHashMap<>();
    private final Map<String, Supplier<PlayerTransfer>> transfers = new ConcurrentHashMap<>();

    public void registerBus(@NotNull String name, @NotNull Supplier<MessageBus> factory) {
        register(buses, "message bus", name, factory);
    }

    public void registerDirectory(@NotNull String name, @NotNull Supplier<SiteDirectory> factory) {
        register(directories, "site directory", name, factory);
    }

    public void registerTransfer(@NotNull String name, @NotNull Supplier<PlayerTransfer> factory) {
        register(transfers, "player transfer", name, factory);
    }

    public @NotNull Optional<MessageBus> bus(@NotNull String name) {
        return resolve(buses, name);
    }

    public @NotNull Optional<SiteDirectory> directory(@NotNull String name) {
        return resolve(directories, name);
    }

    public @NotNull Optional<PlayerTransfer> transfer(@NotNull String name) {
        return resolve(transfers, name);
    }

    public @NotNull Set<String> busNames() {
        return Set.copyOf(buses.keySet());
    }

    public @NotNull Set<String> directoryNames() {
        return Set.copyOf(directories.keySet());
    }

    public @NotNull Set<String> transferNames() {
        return Set.copyOf(transfers.keySet());
    }

    private <T> void register(@NotNull Map<String, Supplier<T>> into, @NotNull String seam, @NotNull String name,
                              @NotNull Supplier<T> factory) {
        final String key = key(name);
        if (into.put(key, factory) != null) {
            log.warn("A second {} named '{}' replaced the first", seam, key).submit();
        }
    }

    private <T> @NotNull Optional<T> resolve(@NotNull Map<String, Supplier<T>> from, @NotNull String name) {
        return Optional.ofNullable(from.get(key(name))).map(Supplier::get);
    }

    private @NotNull String key(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
