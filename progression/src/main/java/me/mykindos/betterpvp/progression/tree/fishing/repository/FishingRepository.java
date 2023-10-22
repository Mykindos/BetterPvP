package me.mykindos.betterpvp.progression.tree.fishing.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.stats.ProgressionStatsRepository;
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
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingConfigLoader;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.rod.SimpleFishingRod;
import org.bukkit.configuration.ConfigurationSection;
import org.reflections.Reflections;

import javax.sql.rowset.CachedRowSet;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// All data for players should be loaded for as long as they are on, and saved when they log off
// The data should be saved as a fallback to the database every 5 minutes
// The data should be saved on shutdown
@Slf4j
@Singleton
public class FishingRepository extends ProgressionStatsRepository<Fishing, FishingData> implements ConfigAccessor {

    @Getter
    private final WeighedList<FishingLootType> lootTypes = new WeighedList<>();
    @Getter
    private final Set<FishingRodType> rodTypes = new HashSet<>();
    @Getter
    private final Set<BaitType> baitTypes = new HashSet<>();
    private final FishingConfigLoader<?>[] baitLoaders = new FishingConfigLoader<?>[]{
            new SpeedBaitLoader()
    };
    private final FishingConfigLoader<?>[] lootLoaders = new FishingConfigLoader<?>[]{
            new SwimmerLoader(),
            new FishTypeLoader(),
            new TreasureLoader()
    };

    @Inject
    public FishingRepository(Progression progression) {
        super(progression, "Fishing");
    }

    @Override
    public CompletableFuture<FishingData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String stmt = "SELECT COUNT(*), SUM(Weight) FROM " + plugin.getDatabasePrefix() + "fishing WHERE gamer = ?;";
                final Statement query = new Statement(stmt, new StringStatementValue(player.toString()));
                final FishingData data = new FishingData();
                final CachedRowSet result = database.executeQuery(query);
                if (result.next()) {
                    data.setFishCaught(result.getInt(1));
                    data.setWeightCaught(result.getInt(2));
                }
                return data;
            } catch (SQLException e) {
                log.error("Failed to get fishing data for player " + player, e);
            }
            return new FishingData();
        }).exceptionally(throwable -> {
            log.error("Failed to get fishing data for player " + player, throwable);
            return null;
        });
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        // Clear
        lootTypes.clear();
        rodTypes.clear();
        baitTypes.clear();

        Reflections classScan = new Reflections(Fishing.class.getPackageName());
        loadLootTypes(classScan, config);
        loadBaitTypes(classScan, config);
        loadRodTypes(classScan, config);
    }

    private void loadRodTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends FishingRodType>> rodClasses = reflections.getSubTypesOf(FishingRodType.class);
        rodClasses.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        rodClasses.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));
        for (var clazz : rodClasses) {
            FishingRodType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);
            type.loadConfig(config);
            rodTypes.add(type);
        }
        rodTypes.addAll(List.of(SimpleFishingRod.values()));
        log.info("Loaded " + rodTypes.size() + " rod types");
    }

    private void loadBaitTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends BaitType>> baitClasses = reflections.getSubTypesOf(BaitType.class);
        baitClasses.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        baitClasses.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));
        baitClasses.removeIf(clazz -> clazz == SimpleBaitType.class); // Skip config fish type
        baitClasses.removeIf(clazz -> clazz == SpeedBaitType.class); // Skip config fish type
        for (var clazz : baitClasses) {
            BaitType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);

            type.loadConfig(config);
            baitTypes.add(type);
        }

        ConfigurationSection customBaitSection = config.getConfigurationSection("fishing.bait");
        if (customBaitSection == null) {
            customBaitSection = config.createSection("fishing.bait");
        }

        for (String key : customBaitSection.getKeys(false)) {
            final ConfigurationSection section = customBaitSection.getConfigurationSection(key);
            final String type = Objects.requireNonNull(section).getString("type");

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
    }

    private void loadLootTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends FishingLootType>> classes = reflections.getSubTypesOf(FishingLootType.class);
        classes.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        classes.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));
        classes.removeIf(clazz -> clazz == SimpleFishType.class); // Skip config fish type
        classes.removeIf(clazz -> clazz == SwimmerType.class); // Skip config fish type
        classes.removeIf(clazz -> clazz == TreasureType.class); // Skip config fish type
        for (var clazz : classes) {
            FishingLootType type = plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(type);

            // We do a weight of 1 because we want fish with the same frequency to be equally likely
            type.loadConfig(config);
            lootTypes.add(type.getFrequency(), 1, type);
        }

        ConfigurationSection customFishSection = config.getConfigurationSection("fishing.loot");
        if (customFishSection == null) {
            customFishSection = config.createSection("fishing.loot");
        }

        for (String key : customFishSection.getKeys(false)) {
            final ConfigurationSection section = customFishSection.getConfigurationSection(key);
            final String type = Objects.requireNonNull(section).getString("type");

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
    }

    @Override
    protected Class<Fishing> getTreeClass() {
        return Fishing.class;
    }
}
