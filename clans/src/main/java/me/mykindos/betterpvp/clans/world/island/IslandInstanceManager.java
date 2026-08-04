package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every live {@link IslandInstance}, provisioning and destroying their worlds through
 * {@link IslandWorldProvisioner} and deciding, via the injected {@link InstanceAllocationPolicy}, whether a player
 * should join an existing instance or get a fresh one. Every allocation and release is mirrored to the
 * {@link IslandInstanceRepository} so {@link IslandBootRecovery} can clean up leftovers after a restart.
 */
@CustomLog
@Singleton
public class IslandInstanceManager {

    private final Map<UUID, IslandInstance> instances = new ConcurrentHashMap<>();
    private final IslandWorldProvisioner provisioner;
    private final InstanceAllocationPolicy allocationPolicy;
    private final IslandInstanceRepository repository;
    private final Provider<IslandWarmPool> warmPool; // circular dependency: the pool registers instances back into this manager

    @Inject
    public IslandInstanceManager(@NotNull IslandWorldProvisioner provisioner, @NotNull InstanceAllocationPolicy allocationPolicy,
                                  @NotNull IslandInstanceRepository repository, @NotNull Provider<IslandWarmPool> warmPool) {
        this.provisioner = provisioner;
        this.allocationPolicy = allocationPolicy;
        this.repository = repository;
        this.warmPool = warmPool;
    }

    /**
     * Hands the caller an instance for {@code template} as fast as possible: a {@code POOLED} instance is claimed
     * instantly if the warm pool has one, otherwise a fresh world is provisioned as before. Either path triggers a
     * background refill afterwards so the pool recovers what it just gave up.
     */
    public @NotNull CompletableFuture<IslandInstance> allocate(@NotNull IslandTemplate template) {
        final Optional<IslandInstance> pooled = warmPool.get().claim(template);
        if (pooled.isPresent()) {
            final IslandInstance instance = pooled.get();
            log.info("Claimed pooled island instance {} ({}) at world '{}'", instance.getId(), template.getKey(), instance.getWorldName()).submit();
            warmPool.get().refill();
            return CompletableFuture.completedFuture(instance);
        }

        final UUID id = UUID.randomUUID();
        return provisioner.provision(template, id).thenApply((World world) -> {
            final IslandInstance instance = new IslandInstance(id, template, world.getName());
            instance.setState(IslandInstanceState.READY);
            register(instance);
            log.info("Allocated island instance {} ({}) at world '{}'", id, template.getKey(), world.getName()).submit();
            warmPool.get().refill();
            return instance;
        });
    }

    /** Adds a freshly provisioned instance to the live registry and persists it. Used by both {@link #allocate} and {@link IslandWarmPool}. */
    public void register(@NotNull IslandInstance instance) {
        instances.put(instance.getId(), instance);
        repository.save(instance);
    }

    public @NotNull Optional<IslandInstance> find(@NotNull UUID id) {
        return Optional.ofNullable(instances.get(id));
    }

    public @NotNull Optional<IslandInstance> byWorld(@NotNull String worldName) {
        return instances.values().stream().filter(instance -> instance.getWorldName().equals(worldName)).findFirst();
    }

    /** The instance a player is counted as occupying, which is the only record of where a delivered traveller went. */
    public @NotNull Optional<IslandInstance> byOccupant(@NotNull UUID player) {
        return instances.values().stream().filter(instance -> instance.getOccupants().contains(player)).findFirst();
    }

    public @NotNull Collection<IslandInstance> all() {
        return instances.values();
    }

    /**
     * Resolves {@code prefix} against every live instance's id, case-insensitively. Callers decide how to handle an
     * empty (no match) or multi-element (ambiguous) result — this only performs the lookup.
     */
    public @NotNull List<IslandInstance> matchIdPrefix(@NotNull String prefix) {
        final String lower = prefix.toLowerCase();
        return instances.values().stream()
                .filter(instance -> instance.getId().toString().toLowerCase().startsWith(lower))
                .toList();
    }

    /** Equivalent to {@link #release(UUID, boolean)} with {@code force = false}. */
    public @NotNull CompletableFuture<Void> release(@NotNull UUID id) {
        return release(id, false);
    }

    /**
     * Destroys the instance's world and drops it from the registry. Refuses to do so while it still has occupants
     * unless {@code force} is set.
     */
    public @NotNull CompletableFuture<Void> release(@NotNull UUID id, boolean force) {
        final IslandInstance instance = instances.get(id);
        if (instance == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (!force && !instance.isEmpty()) {
            log.warn("Refusing to release island instance {} - still has {} occupant(s)", id, instance.getOccupants().size()).submit();
            return CompletableFuture.completedFuture(null);
        }

        instance.setState(IslandInstanceState.RELEASING);
        return provisioner.destroy(instance.getWorldName()).thenRun(() -> {
            instances.remove(id);
            repository.delete(id);
            log.info("Released island instance {}", id).submit();
        });
    }

    /** @return an existing instance this player may join under the current policy, or empty to allocate a fresh one */
    public @NotNull Optional<IslandInstance> offerTo(@NotNull Player player) {
        return instances.values().stream()
                .filter(instance -> instance.getState() == IslandInstanceState.READY)
                .filter(instance -> allocationPolicy.canAccept(instance, player))
                .findFirst();
    }

    public void enter(@NotNull IslandInstance instance, @NotNull Player player) {
        instance.addOccupant(player.getUniqueId());
    }

    public void exit(@NotNull IslandInstance instance, @NotNull Player player) {
        instance.removeOccupant(player.getUniqueId());
        instance.setLastVacatedAt(System.currentTimeMillis());
    }

}
