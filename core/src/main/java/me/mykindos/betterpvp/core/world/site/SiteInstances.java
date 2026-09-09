package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every live instance on this server, and the one way to get one. {@link #locate(SiteKey, Party)} answers "where does
 * this party go?" for any kind of site, provisioning, waking or overflowing as the site's policy requires.
 */
@CustomLog
@BPvPListener
@Singleton
public class SiteInstances implements Listener {

    private final Map<UUID, SiteInstance> instances = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> recovered = new CompletableFuture<>();
    private final SiteRegistry registry;
    private final SiteWorlds worlds;
    private final SiteStore store;

    @Inject
    public SiteInstances(@NotNull SiteRegistry registry, @NotNull SiteWorlds worlds, @NotNull SiteStore store) {
        this.registry = registry;
        this.worlds = worlds;
        this.store = store;
    }

    /**
     * Completes once the previous run has been reconciled and orphaned folders swept. Anything that provisions worlds
     * at startup chains onto this so it never writes into a folder the sweep is still deciding about.
     */
    public @NotNull CompletableFuture<Void> whenRecovered() {
        return recovered;
    }

    @EventHandler
    public void onServerStart(@NotNull ServerStartEvent event) {
        store.recover()
                .thenAccept(survivors -> survivors.forEach(instance -> instances.put(instance.getId(), instance)))
                .whenComplete((unused, ex) -> {
                    if (ex != null) {
                        log.error("Site recovery failed", ex).submit();
                    }
                    recovered.complete(null);
                    refill();
                });
    }

    /**
     * Finds the instance a whole party should travel to, in a fixed order: an existing instance that admits them, a
     * dormant one to wake, a new one if the site may grow, and failing all of that the site's fallback. The party is
     * counted as occupants from here on, so a second party asking at the same moment sees them.
     */
    public @NotNull CompletableFuture<SiteInstance> locate(@NotNull SiteKey key, @NotNull Party party) {
        return locate(key, party, new HashSet<>());
    }

    /** Releases a reservation made by {@link #locate} for a journey that never happened. */
    public void abandon(@NotNull SiteInstance instance, @NotNull Party party) {
        party.getMembers().forEach(instance::removeOccupant);
    }

    public @NotNull Optional<SiteInstance> find(@NotNull UUID id) {
        return Optional.ofNullable(instances.get(id));
    }

    public @NotNull Optional<SiteInstance> byWorld(@NotNull String worldName) {
        return instances.values().stream().filter(instance -> instance.getWorldName().equals(worldName)).findFirst();
    }

    /** The instance a player is counted as occupying, which is the only record of where a traveller went. */
    public @NotNull Optional<SiteInstance> byOccupant(@NotNull UUID player) {
        return instances.values().stream().filter(instance -> instance.getOccupants().contains(player)).findFirst();
    }

    public @NotNull Collection<SiteInstance> all() {
        return instances.values();
    }

    public @NotNull List<SiteInstance> forKey(@NotNull SiteKey key) {
        return instances.values().stream().filter(instance -> instance.getKey().equals(key)).toList();
    }

    /**
     * Resolves a prefix against every live instance id, case-insensitively. Callers decide how to handle an empty (no
     * match) or multi-element (ambiguous) result.
     */
    public @NotNull List<SiteInstance> matchIdPrefix(@NotNull String prefix) {
        final String lower = prefix.toLowerCase(Locale.ROOT);
        return instances.values().stream()
                .filter(instance -> instance.getId().toString().toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }

    public void enter(@NotNull SiteInstance instance, @NotNull UUID player) {
        instance.addOccupant(player);
    }

    public void exit(@NotNull SiteInstance instance, @NotNull UUID player) {
        instance.removeOccupant(player);
    }

    /**
     * Unloads an instance's world but keeps its folder, so it can be woken by the next party to ask for it. Only
     * meaningful for a site whose world survives being empty.
     */
    public @NotNull CompletableFuture<Void> sleep(@NotNull UUID id) {
        final SiteInstance instance = instances.get(id);
        if (instance == null || instance.getState() != SiteInstance.State.READY) {
            return CompletableFuture.completedFuture(null);
        }

        instance.setState(SiteInstance.State.RELEASING);
        return worlds.unload(instance.getWorldName()).thenRun(() -> {
            instance.setState(SiteInstance.State.DORMANT);
            store.updateState(id, SiteInstance.State.DORMANT);
            log.info("Site instance {} ({}) is dormant", id, instance.getKey()).submit();
        });
    }

    /** Equivalent to {@link #release(UUID, boolean)} with {@code force} unset. */
    public @NotNull CompletableFuture<Void> release(@NotNull UUID id) {
        return release(id, false);
    }

    /**
     * Destroys an instance's world and drops it from the registry. Refuses while it still has occupants unless
     * {@code force} is set.
     */
    public @NotNull CompletableFuture<Void> release(@NotNull UUID id, boolean force) {
        final SiteInstance instance = instances.get(id);
        if (instance == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (!force && !instance.isEmpty()) {
            log.warn("Refusing to release site instance {} - still has {} occupant(s)", id, instance.getOccupants().size()).submit();
            return CompletableFuture.completedFuture(null);
        }

        instance.setState(SiteInstance.State.RELEASING);
        return worlds.destroy(instance.getWorldName()).thenRun(() -> {
            instances.remove(id);
            store.delete(id);
            log.info("Released site instance {} ({})", id, instance.getKey()).submit();
        });
    }

    /**
     * Provisions each site up to the number of instances it keeps ready ahead of demand, so a party arriving at a
     * pooled site walks into a world that already exists rather than waiting on a clone.
     */
    public void refill() {
        for (Site site : registry.all()) {
            final SiteKey key = site.key();
            if (key.isOwned() || site.getPolicy().getMin() <= 0) {
                continue;
            }

            // provision() registers the instance before its world exists, so the count below is accurate
            // immediately and a second pass never doubles up on one that is still being cloned.
            for (int missing = site.getPolicy().getMin() - forKey(key).size(); missing > 0; missing--) {
                provision(site, key).exceptionally(ex -> {
                    log.warn("Failed to provision instance for site {}", key, ex).submit();
                    return null;
                });
            }
        }
    }

    /**
     * Retires instances that have sat empty past their site's grace period, destroying the ones whose worlds are
     * disposable and unloading the ones worth keeping. Instances a site holds ready are left alone.
     */
    @UpdateEvent(delay = 10_000L)
    public void retireEmptyInstances() {
        final long now = System.currentTimeMillis();

        for (SiteInstance instance : List.copyOf(instances.values())) {
            final Optional<Site> site = registry.get(instance.getKey().getSiteId());
            if (site.isEmpty() || !isRetirable(instance, site.get(), now)) {
                continue;
            }

            if (site.get().getPolicy().reapsWhenEmpty()) {
                release(instance.getId());
            } else {
                sleep(instance.getId());
            }
        }

        refill();
    }

    private boolean isRetirable(@NotNull SiteInstance instance, @NotNull Site site, long now) {
        if (instance.getState() != SiteInstance.State.READY || !instance.isEmpty()) {
            return false;
        }

        final SitePolicy policy = site.getPolicy();
        if (!policy.reapsWhenEmpty() && policy.getDormancy() == SitePolicy.Dormancy.ALWAYS_LOADED) {
            return false;
        }

        // Never retire an instance the site is meant to be holding ready, or a warm instance would be reaped the
        // moment it was provisioned and replaced by another for the same treatment.
        if (forKey(instance.getKey()).size() <= policy.getMin()) {
            return false;
        }

        return now - instance.getLastVacatedAt() >= policy.getDormancyGraceSeconds() * 1000L;
    }

    private @NotNull CompletableFuture<SiteInstance> locate(@NotNull SiteKey key, @NotNull Party party, @NotNull Set<String> visited) {
        final Optional<Site> found = registry.get(key.getSiteId());
        if (found.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No such site: " + key));
        }

        final Site site = found.get();
        if (!visited.add(site.getId())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Site fallbacks loop through " + site.getId()));
        }

        final SitePolicy policy = site.getPolicy();
        final List<SiteInstance> live = forKey(key);

        final List<SiteInstance> ready = live.stream()
                .filter(instance -> instance.getState() == SiteInstance.State.READY)
                .filter(instance -> hasRoom(instance, policy, party))
                .filter(instance -> policy.getAdmission().admits(key, instance.getOccupants(), party))
                .toList();

        if (!ready.isEmpty()) {
            final int chosen = policy.getSelection().select(ready.stream().map(instance -> instance.getOccupants().size()).toList());
            return CompletableFuture.completedFuture(reserve(ready.get(chosen), party));
        }

        final Optional<SiteInstance> dormant = live.stream()
                .filter(instance -> instance.getState() == SiteInstance.State.DORMANT)
                .filter(instance -> policy.getAdmission().admits(key, instance.getOccupants(), party))
                .findFirst();

        if (dormant.isPresent()) {
            return wake(site, dormant.get()).thenApply(instance -> reserve(instance, party));
        }

        if (policy.getMax() <= 0 || live.size() < policy.getMax()) {
            return provision(site, key).thenApply(instance -> reserve(instance, party));
        }

        final String fallback = policy.getFallbackSiteId();
        if (fallback == null) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Site " + key + " is full and has no fallback"));
        }

        log.info("Site {} is full, sending party of {} to '{}'", key, party.size(), fallback).submit();
        return locate(SiteKey.of(fallback), party, visited);
    }

    private boolean hasRoom(@NotNull SiteInstance instance, @NotNull SitePolicy policy, @NotNull Party party) {
        if (policy.getCapacity() <= 0) {
            return true;
        }

        final Set<UUID> arriving = new HashSet<>(party.getMembers());
        arriving.removeAll(instance.getOccupants());
        return instance.getOccupants().size() + arriving.size() <= policy.getCapacity();
    }

    private @NotNull SiteInstance reserve(@NotNull SiteInstance instance, @NotNull Party party) {
        party.getMembers().forEach(instance::addOccupant);
        return instance;
    }

    private @NotNull CompletableFuture<SiteInstance> wake(@NotNull Site site, @NotNull SiteInstance instance) {
        instance.setState(SiteInstance.State.PROVISIONING);
        return worlds.open(site, instance.getWorldName()).thenApply(world -> {
            instance.setState(SiteInstance.State.READY);
            store.updateState(instance.getId(), SiteInstance.State.READY);
            log.info("Woke site instance {} ({}) at world '{}'", instance.getId(), instance.getKey(), world.getName()).submit();
            return instance;
        });
    }

    /**
     * Creates a new instance and its world. The instance is registered before its world exists so that concurrent
     * callers count it against the site's maximum rather than racing to provision a second one.
     */
    private @NotNull CompletableFuture<SiteInstance> provision(@NotNull Site site, @NotNull SiteKey key) {
        final UUID id = UUID.randomUUID();
        final String worldName = worlds.worldNameFor(site, key, id);
        final SiteInstance instance = new SiteInstance(id, key, worldName, SiteInstance.State.PROVISIONING);
        instances.put(id, instance);

        return worlds.open(site, worldName).thenApply(world -> {
            instance.setState(SiteInstance.State.READY);
            store.save(instance);
            log.info("Provisioned site instance {} ({}) at world '{}'", id, key, world.getName()).submit();
            return instance;
        }).exceptionallyCompose(ex -> {
            instances.remove(id);
            return CompletableFuture.failedFuture(ex);
        });
    }
}
