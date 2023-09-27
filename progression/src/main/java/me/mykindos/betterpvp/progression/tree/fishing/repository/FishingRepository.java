package me.mykindos.betterpvp.progression.tree.fishing.repository;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionRepository;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingData;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.rod.SimpleFishingRod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// All data for players should be loaded for as long as they are on, and saved when they log off
// The data should be saved as a fallback to the database every 5 minutes
// The data should be saved on shutdown
@Slf4j
@Singleton
@BPvPListener
public class FishingRepository implements ProgressionRepository<Fishing, FishingData>, Listener, ConfigAccessor {

    @Getter
    private final WeighedList<FishType> fishTypes = new WeighedList<>();
    @Getter
    private final Set<FishingRodType> rodTypes = new HashSet<>();
    private final AsyncLoadingCache<UUID, FishingData> dataCache;

    private final Database database;
    private final Progression plugin;

    @Inject
    public FishingRepository(Progression plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.dataCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .evictionListener((UUID uuid, FishingData data, RemovalCause cause) -> save(uuid))
                .buildAsync((AsyncCacheLoader<? super UUID, FishingData>) ((key, executor) -> loadOrCreate(key)));

        reloadTypes();
    }

    private void reloadTypes() {
        // Clear
        fishTypes.clear();
        rodTypes.clear();

        // Load fish types
        Reflections reflections = new Reflections(Fishing.class.getPackageName());
        Set<Class<? extends FishType>> classes = reflections.getSubTypesOf(FishType.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FishType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            // We do a weight of 1 because we want fish with the same frequency to be equally likely
            fishTypes.add(type.getFrequency(), 1, type);
        }
        log.info("Loaded " + fishTypes.size() + " fish types");

        Set<Class<? extends FishingRodType>> rodClasses = reflections.getSubTypesOf(FishingRodType.class);
        for (var clazz : rodClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FishingRodType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            rodTypes.add(type);
        }
        rodTypes.addAll(List.of(SimpleFishingRod.values()));
        log.info("Loaded " + rodTypes.size() + " rod types");
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        fishTypes.forEach(type -> plugin.getInjector().injectMembers(type));
    }

    @Override
    public CompletableFuture<FishingData> getData(UUID player) {
        return dataCache.get(player);
    }

    @Override
    public CompletableFuture<FishingData> loadOrCreate(UUID player) {
        return CompletableFuture.completedFuture(new FishingData()); // todo load from or put in database
    }

    @Override
    public void save(UUID player) {
        // todo save to database
    }

    @Override
    public void save() {
        // todo: save batch
        dataCache.asMap().forEach((uuid, data) -> {
        });
    }

    @Override
    public void shutdown() {
        save();
        dataCache.synchronous().invalidateAll();
    }

    // Save everything every 5 minutes
    @UpdateEvent(delay = 20 * 60 * 5, isAsync = true)
    public void cycleSave() {
        save();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        save(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        loadOrCreate(event.getPlayer().getUniqueId());
    }

}
