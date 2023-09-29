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
import me.mykindos.betterpvp.progression.tree.fishing.bait.SimpleBaitType;
import me.mykindos.betterpvp.progression.tree.fishing.bait.speed.SpeedBaitLoader;
import me.mykindos.betterpvp.progression.tree.fishing.bait.speed.SpeedBaitType;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingData;
import me.mykindos.betterpvp.progression.tree.fishing.fish.FishTypeLoader;
import me.mykindos.betterpvp.progression.tree.fishing.fish.SimpleFishType;
import me.mykindos.betterpvp.progression.tree.fishing.loot.SwimmerLoader;
import me.mykindos.betterpvp.progression.tree.fishing.loot.SwimmerType;
import me.mykindos.betterpvp.progression.tree.fishing.loot.TreasureLoader;
import me.mykindos.betterpvp.progression.tree.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.tree.fishing.model.BaitType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingConfigLoader;
import me.mykindos.betterpvp.progression.tree.fishing.rod.SimpleFishingRod;
import org.bukkit.configuration.ConfigurationSection;
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
    private final WeighedList<FishingLootType> lootTypes = new WeighedList<>();
    @Getter
    private final Set<FishingRodType> rodTypes = new HashSet<>();
    @Getter
    private final Set<BaitType> baitTypes = new HashSet<>();
    private final AsyncLoadingCache<UUID, FishingData> dataCache;
    private final FishingConfigLoader<?>[] baitLoaders = new FishingConfigLoader<?>[]{
            new SpeedBaitLoader()
    };
    private final FishingConfigLoader<?>[] lootLoaders = new FishingConfigLoader<?>[]{
            new SwimmerLoader(),
            new FishTypeLoader(),
            new TreasureLoader()
    };

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
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        // Clear
        lootTypes.clear();
        rodTypes.clear();
        baitTypes.clear();

        // Load fish types
        Reflections reflections = new Reflections(Fishing.class.getPackageName());
        Set<Class<? extends FishingLootType>> classes = reflections.getSubTypesOf(FishingLootType.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            if (clazz == SimpleFishType.class) continue; // Skip config fish type
            if (clazz == SwimmerType.class) continue; // Skip config fish type
            if (clazz == TreasureType.class) continue;
            FishingLootType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);

            // We do a weight of 1 because we want fish with the same frequency to be equally likely
            type.loadConfig(config);
            lootTypes.add(type.getFrequency(), 1, type);
        }

        // Create dynamic loot types
        ConfigurationSection customFishSection = config.getConfigurationSection("fishing.loot");
        if (customFishSection == null) {
            customFishSection = config.createSection("fishing.loot");
        }

        for (String key : customFishSection.getKeys(false)) {
            final ConfigurationSection section = customFishSection.getConfigurationSection(key);
            final String type = section.getString("type");

            boolean found = false;
            for (FishingConfigLoader<?> loader : lootLoaders) {
                if (loader.getTypeKey().equalsIgnoreCase(type)) {
                    final FishingLootType loaded = (FishingLootType) loader.read(section);
                    loaded.loadConfig(config);
                    lootTypes.add(loaded.getFrequency(), 1, loaded);
                    found = true;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Unknown loot type: " + type);
            }
        }
        log.info("Loaded " + lootTypes.size() + " loot types");

        // Load bait types
        Set<Class<? extends BaitType>> baitClasses = reflections.getSubTypesOf(BaitType.class);
        for (var clazz : baitClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            if (clazz == SimpleBaitType.class) continue; // Skip config fish type
            if (clazz == SpeedBaitType.class) continue; // Skip config fish type
            BaitType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);

            type.loadConfig(config);
            baitTypes.add(type);
        }

        // Create dynamic loot types
        ConfigurationSection customBaitSection = config.getConfigurationSection("fishing.bait");
        if (customBaitSection == null) {
            customBaitSection = config.createSection("fishing.bait");
        }

        for (String key : customBaitSection.getKeys(false)) {
            final ConfigurationSection section = customBaitSection.getConfigurationSection(key);
            final String type = section.getString("type");

            boolean found = false;
            for (FishingConfigLoader<?> loader : baitLoaders) {
                if (loader.getTypeKey().equalsIgnoreCase(type)) {
                    final BaitType loaded = (BaitType) loader.read(section);
                    loaded.loadConfig(config);
                    baitTypes.add(loaded);
                    found = true;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Unknown bait type: " + type);
            }
        }
        log.info("Loaded " + baitTypes.size() + " bait types");

        Set<Class<? extends FishingRodType>> rodClasses = reflections.getSubTypesOf(FishingRodType.class);
        for (var clazz : rodClasses) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FishingRodType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            type.loadConfig(config);
            rodTypes.add(type);
        }
        rodTypes.addAll(List.of(SimpleFishingRod.values()));
        log.info("Loaded " + rodTypes.size() + " rod types");
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
