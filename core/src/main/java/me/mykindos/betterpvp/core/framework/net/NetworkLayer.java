package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Resolves each network seam once, from config, and holds what it resolved.
 * <p>
 * Every seam is chosen the same way: a name from config, looked up in {@link NetworkTransports}. The names that ship
 * are {@code proxy}, {@code redis} and {@code local}, and anything another network registers stands beside them on
 * equal terms. This is the only class that chooses, so joining a different network is a registration and a config
 * value, never an edit to whatever is using a seam.
 */
@Singleton
@CustomLog
public class NetworkLayer {

    public static final String PROXY = "proxy";
    public static final String REDIS = "redis";
    public static final String LOCAL = "local";

    private final Core core;
    private final NetworkTransports transports;
    private final RedisConnection redis;

    private volatile MessageBus bus;
    private volatile SiteDirectory directory;
    private volatile PlayerTransfer transfer;

    @Inject
    public NetworkLayer(@NotNull Core core, @NotNull NetworkTransports transports, @NotNull RedisConnection redis,
                        @NotNull Provider<VelocityMessageBus> proxyBus,
                        @NotNull Provider<LocalSiteDirectory> localDirectory,
                        @NotNull Provider<ProxyPlayerTransfer> proxyTransfer) {
        this.core = core;
        this.transports = transports;
        this.redis = redis;

        // What ships, registered the same way anything else would be. A factory answering null means that
        // implementation cannot run here, which is how Redis withdraws when it is not configured.
        transports.registerBus(PROXY, proxyBus::get);
        transports.registerBus(REDIS, () -> redis.isUsable() ? startedRedisBus() : null);
        transports.registerDirectory(LOCAL, localDirectory::get);
        transports.registerDirectory(REDIS, () -> redis.isUsable() ? new RedisSiteDirectory(core, redis.getPool()) : null);
        transports.registerTransfer(PROXY, proxyTransfer::get);
    }

    /** How servers talk to each other. */
    public @NotNull MessageBus bus() {
        if (bus == null) {
            synchronized (this) {
                if (bus == null) {
                    bus = choose("bus", transports::bus, List.of(REDIS, PROXY), "message bus");
                }
            }
        }
        return bus;
    }

    /** What instances exist across the network. */
    public @NotNull SiteDirectory directory() {
        if (directory == null) {
            synchronized (this) {
                if (directory == null) {
                    directory = choose("directory", transports::directory, List.of(REDIS, LOCAL), "site directory");
                }
            }
        }
        return directory;
    }

    /** How a player gets from this server to another. */
    public @NotNull PlayerTransfer transfer() {
        if (transfer == null) {
            synchronized (this) {
                if (transfer == null) {
                    transfer = choose("transfer", transports::transfer, List.of(PROXY), "player transfer");
                }
            }
        }
        return transfer;
    }

    public void close() {
        if (bus != null) {
            bus.close();
        }
        if (directory != null) {
            directory.close();
        }
        redis.close();
    }

    /**
     * Takes what config named, or the first of {@code preferred} that can run here.
     *
     * @param seam      the config key under {@code core.network}
     * @param registry  where to look a name up
     * @param preferred what to try when config says to work it out, best first
     */
    private <T> @NotNull T choose(@NotNull String seam, @NotNull Lookup<T> registry, @NotNull List<String> preferred,
                                  @NotNull String describedAs) {
        final String named = core.getConfig().getOrSaveString("core.network." + seam, NetworkTransports.AUTO);

        if (!NetworkTransports.AUTO.equalsIgnoreCase(named)) {
            final Optional<T> chosen = registry.get(named);
            if (chosen.isPresent()) {
                log.info("Using the '{}' {}", named, describedAs).submit();
                return chosen.get();
            }

            // Said out loud, because falling back quietly is how a network runs a season on the wrong one.
            log.warn("No {} named '{}' could be used, falling back", describedAs, named).submit();
        }

        for (String candidate : preferred) {
            final Optional<T> chosen = registry.get(candidate);
            if (chosen.isPresent()) {
                log.info("Using the '{}' {}", candidate, describedAs).submit();
                return chosen.get();
            }
        }

        throw new IllegalStateException("No " + describedAs + " is available, which should be impossible");
    }

    private @NotNull MessageBus startedRedisBus() {
        final RedisMessageBus redisBus = new RedisMessageBus(core, redis.getPool(), redis.channel());
        redisBus.start();
        return redisBus;
    }

    @FunctionalInterface
    private interface Lookup<T> {
        @NotNull Optional<T> get(@NotNull String name);
    }
}
