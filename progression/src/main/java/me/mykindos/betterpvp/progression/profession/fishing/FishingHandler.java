package me.mykindos.betterpvp.progression.profession.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.fishing.bait.SimpleBaitType;
import me.mykindos.betterpvp.progression.profession.fishing.bait.speed.SpeedBaitLoader;
import me.mykindos.betterpvp.progression.profession.fishing.bait.speed.SpeedBaitType;
import me.mykindos.betterpvp.progression.profession.fishing.data.CaughtFish;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profession.fishing.fish.FishTypeLoader;
import me.mykindos.betterpvp.progression.profession.fishing.fish.SimpleFishType;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.BiggestFishLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.FishingCountLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.leaderboards.FishingWeightLeaderboard;
import me.mykindos.betterpvp.progression.profession.fishing.loot.SwimmerLoader;
import me.mykindos.betterpvp.progression.profession.fishing.loot.SwimmerType;
import me.mykindos.betterpvp.progression.profession.fishing.loot.TreasureLoader;
import me.mykindos.betterpvp.progression.profession.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.profession.fishing.model.BaitType;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingConfigLoader;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.profession.fishing.repository.FishingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
@CustomLog
public class FishingHandler extends ProfessionHandler {

    @Inject
    @Config(path = "fishing.xpPerPound", defaultValue = "0.10")
    private double xpPerPound;

    @Getter
    private final FishingRepository fishingRepository;
    private final LeaderboardManager leaderboardManager;

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
    protected FishingHandler(Progression progression, ProfessionProfileManager professionProfileManager, FishingRepository fishingRepository, LeaderboardManager leaderboardManager) {
        super(progression, professionProfileManager, "Fishing");
        this.fishingRepository = fishingRepository;
        this.leaderboardManager = leaderboardManager;
    }

    public void addFish(Player player, Fish fish) {

        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) return;

        double xp = (fish.getWeight() * xpPerPound);
        if (xp > 0) {
            professionData.grantExperience(xp, player);
        }

        log.info("{} caught a {} pound {} for {} experience", player.getName(), fish.getWeight(), fish.getType().getName(), xp)
                .addClientContext(player).addLocationContext(player.getLocation())
                .addContext("Experience", xp + "").addContext("Fish Weight", fish.getWeight() + "").submit();

        fishingRepository.saveFish(player.getUniqueId(), fish);

        long fishCaught = (long) professionData.getProperties().getOrDefault("TOTAL_FISH_CAUGHT", 0L);
        professionData.getProperties().put("TOTAL_FISH_CAUGHT", fishCaught + 1);

        long weightCaught = (long) professionData.getProperties().getOrDefault("TOTAL_WEIGHT_CAUGHT", 0L);
        professionData.getProperties().put("TOTAL_WEIGHT_CAUGHT", weightCaught + fish.getWeight());


        leaderboardManager.getObject("Total Weight Caught").ifPresent(leaderboard -> {
            FishingWeightLeaderboard fishingWeightLeaderboard = (FishingWeightLeaderboard) leaderboard;
            fishingWeightLeaderboard.add(player.getUniqueId(), (long) fish.getWeight()).whenComplete((result, throwable3) -> {
                if (throwable3 != null) {
                    log.error("Failed to add weight to leaderboard for player " + player.getName(), throwable3).submit();
                    return;
                }

                fishingWeightLeaderboard.attemptAnnounce(player, result);
            });
        });

        leaderboardManager.getObject("Total Fish Caught").ifPresent(leaderboard -> {
            FishingCountLeaderboard fishingCountLeaderboard = (FishingCountLeaderboard) leaderboard;
            fishingCountLeaderboard.add(player.getUniqueId(), 1L).whenComplete((result, throwable3) -> {
                if (throwable3 != null) {
                    log.error("Failed to add fish count to leaderboard for player " + player.getName(), throwable3).submit();
                    return;
                }

                fishingCountLeaderboard.attemptAnnounce(player, result);
            });
        });

        leaderboardManager.getObject("Biggest Fish Caught").ifPresent(leaderboard -> {
            BiggestFishLeaderboard biggestFishLeaderboard = (BiggestFishLeaderboard) leaderboard;
            biggestFishLeaderboard.add(fish.getUuid(), new CaughtFish(player.getUniqueId(), fish.getType().getName(), fish.getWeight())).whenComplete((result, throwable3) -> {
                if (throwable3 != null) {
                    log.error("Failed to add biggest fish to leaderboard for player " + player.getName(), throwable3).submit();
                    return;
                }

                biggestFishLeaderboard.attemptAnnounce(player, result);
            });
        });

    }

    public Optional<BaitType> getBaitType(ItemStack itemStack) {
        if (itemStack == null) {
            return Optional.empty();
        }

        final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ProgressionNamespacedKeys.FISHING_BAIT_TYPE)) {
            return Optional.empty(); // Default is wooden
        }

        final String type = pdc.get(ProgressionNamespacedKeys.FISHING_BAIT_TYPE, PersistentDataType.STRING);
        return getBaitTypes().stream()
                .filter(baitType -> baitType.getName().equalsIgnoreCase(type))
                .findFirst();
    }


    private void loadBaitTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends BaitType>> baitClasses = reflections.getSubTypesOf(BaitType.class);
        baitClasses.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        baitClasses.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));
        baitClasses.removeIf(clazz -> clazz == SimpleBaitType.class); // Skip config fish type
        baitClasses.removeIf(clazz -> clazz == SpeedBaitType.class); // Skip config fish type
        for (var clazz : baitClasses) {
            BaitType type = progression.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(type);

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
        log.info("Loaded " + baitTypes.size() + " bait types").submit();
    }

    private void loadLootTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends FishingLootType>> classes = reflections.getSubTypesOf(FishingLootType.class);
        classes.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        classes.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));
        classes.removeIf(clazz -> clazz == SimpleFishType.class); // Skip config fish type
        classes.removeIf(clazz -> clazz == SwimmerType.class); // Skip config fish type
        classes.removeIf(clazz -> clazz == TreasureType.class); // Skip config fish type
        for (var clazz : classes) {
            FishingLootType type = progression.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(type);

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
        log.info("Loaded " + lootTypes.size() + " loot types").submit();
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    @Override
    public void loadConfig() {
        super.loadConfig();

        lootTypes.clear();
        rodTypes.clear();
        baitTypes.clear();

        Reflections classScan = new Reflections(this.getClass().getPackageName());
        loadLootTypes(classScan, progression.getConfig());
        loadBaitTypes(classScan, progression.getConfig());

    }
}
