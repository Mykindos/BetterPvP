package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Keeps a small number of pre-provisioned {@code POOLED} island instances on hand per template so
 * {@link IslandInstanceManager#allocate(IslandTemplate)} can hand one out instantly instead of waiting on a world
 * clone. Instances live here only until {@link #claim(IslandTemplate)} flips them to {@code READY}; from that point
 * on the manager owns their lifecycle exactly like a freshly provisioned instance.
 */
@CustomLog
@BPvPListener
@Singleton
public class IslandWarmPool implements Listener {

    private final IslandWorldProvisioner provisioner;
    private final IslandTemplateRegistry templateRegistry;
    private final IslandInstanceManager instanceManager;
    private final IslandInstanceRepository repository;
    private final IslandBootRecovery bootRecovery;
    private final IslandHostRouter router;
    private final Map<String, Deque<IslandInstance>> pools = new ConcurrentHashMap<>();
    private final Set<String> refilling = ConcurrentHashMap.newKeySet();

    @Inject
    @Config(path = "islands.pool.size-per-template", defaultValue = "1")
    private int sizePerTemplate;

    @Inject
    public IslandWarmPool(@NotNull IslandWorldProvisioner provisioner, @NotNull IslandTemplateRegistry templateRegistry,
                           @NotNull IslandInstanceManager instanceManager, @NotNull IslandInstanceRepository repository,
                           @NotNull IslandBootRecovery bootRecovery, @NotNull IslandHostRouter router) {
        this.provisioner = provisioner;
        this.templateRegistry = templateRegistry;
        this.instanceManager = instanceManager;
        this.repository = repository;
        this.bootRecovery = bootRecovery;
        this.router = router;
    }

    /**
     * Runs the initial refill once boot recovery has finished destroying leftover instances and sweeping orphaned
     * world folders. Chaining onto {@link IslandBootRecovery#whenRecovered()} rather than relying on listener
     * registration order or event priority guarantees the pool never provisions into a folder the sweep is still
     * deciding whether to delete.
     */
    @EventHandler
    public void onServerStart(@NotNull ServerStartEvent event) {
        bootRecovery.whenRecovered().thenRun(this::refill);
    }

    /**
     * Atomically takes a {@code POOLED} instance for {@code template} and flips it to {@code READY}. Safe against
     * concurrent callers: the pool is a lock-free deque, so two players claiming at once can never receive the same
     * instance.
     *
     * @return the claimed instance, or empty if the pool for this template is currently drained
     */
    public @NotNull Optional<IslandInstance> claim(@NotNull IslandTemplate template) {
        final Deque<IslandInstance> pool = pools.computeIfAbsent(template.getKey(), key -> new ConcurrentLinkedDeque<>());
        final IslandInstance instance = pool.pollFirst();
        if (instance == null) {
            return Optional.empty();
        }

        instance.setState(IslandInstanceState.READY);
        repository.updateState(instance.getId(), IslandInstanceState.READY);
        return Optional.of(instance);
    }

    /**
     * Provisions up to the configured quota per template, in the background, for every locally-hosted template
     * below it. A template hosted on another server is never pre-provisioned here.
     */
    public void refill() {
        for (IslandTemplate template : templateRegistry.all()) {
            if (router.isLocal(template)) {
                refillTemplate(template);
            }
        }
    }

    /** @return how many {@code POOLED} instances are currently held for {@code templateKey} */
    public int depth(@NotNull String templateKey) {
        return pools.getOrDefault(templateKey, new ConcurrentLinkedDeque<>()).size();
    }

    /** @return the current pool depth for every registered template, in registry order */
    public @NotNull Map<String, Integer> depths() {
        final Map<String, Integer> result = new LinkedHashMap<>();
        for (IslandTemplate template : templateRegistry.all()) {
            result.put(template.getKey(), depth(template.getKey()));
        }
        return result;
    }

    private void refillTemplate(@NotNull IslandTemplate template) {
        final String key = template.getKey();
        if (depth(key) >= sizePerTemplate) {
            return;
        }

        if (!refilling.add(key)) {
            return;
        }

        final UUID id = UUID.randomUUID();
        provisioner.provision(template, id).thenAccept(world -> {
            final IslandInstance instance = new IslandInstance(id, template, world.getName());
            instance.setState(IslandInstanceState.POOLED);
            instanceManager.register(instance);
            pools.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(instance);
            log.info("Pooled island instance {} ({}) at world '{}'", id, key, world.getName()).submit();
            refilling.remove(key);
            refillTemplate(template);
        }).exceptionally(ex -> {
            log.warn("Failed to provision pooled island instance for template {}", key, ex).submit();
            refilling.remove(key);
            return null;
        });
    }

}
