package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** How settlers come to a camp, read from the arrivals, hiring and milestones sections of {@code settlers.yml}. */
@Singleton
@CustomLog
@Getter
public class RecruitConfig implements Reloadable {

    /** The milestone profession that means one rolled from the arrival odds. */
    public static final String ANY = "any";

    @Getter(AccessLevel.NONE)
    private final Clans clans;

    private Duration arrivalEvery = Duration.ofHours(4);
    private Duration arrivalWait = Duration.ofHours(2);
    private Map<Integer, Double> arrivalCounts = Map.of(1, 1.0);
    private SettlerOdds arrivalOdds = new SettlerOdds(Map.of(), Map.of());
    private Map<SettlerRarity, Long> arrivalPrices = Map.of();
    private long campWideTraitPrice;

    private int boardSize = 4;
    private Duration boardRefresh = Duration.ofHours(12);
    private long reroll;
    private SettlerOdds boardOdds = new SettlerOdds(Map.of(), Map.of());
    private Map<SettlerRarity, Long> boardPrices = Map.of();

    /** The settler sent at each clan level, lowest first. */
    private Map<Integer, Milestone> milestones = Map.of();

    @Inject
    public RecruitConfig(@NotNull Clans clans) {
        this.clans = clans;
        clans.getReloadables().add(this);
        reload();
    }

    @Override
    public void reload() {
        final ExtendedYamlConfiguration config = clans.getConfig("settlers");

        arrivalEvery = hours(config.getDouble("arrivals.every-hours", 4));
        arrivalWait = hours(config.getDouble("arrivals.wait-hours", 2));
        final Map<Integer, Double> counts = new LinkedHashMap<>();
        final ConfigurationSection countSection = config.getConfigurationSection("arrivals.count-odds");
        if (countSection != null) {
            for (String count : countSection.getKeys(false)) {
                try {
                    counts.put(Integer.parseInt(count), countSection.getDouble(count));
                } catch (NumberFormatException exception) {
                    log.warn("Arrival count '{}' in settlers.yml is not a number", count).submit();
                }
            }
        }
        arrivalCounts = counts.isEmpty() ? Map.of(1, 1.0) : Map.copyOf(counts);
        arrivalOdds = odds(config, "arrivals");
        arrivalPrices = prices(config.getConfigurationSection("arrivals.prices"));
        campWideTraitPrice = config.getLong("arrivals.common-with-camp-wide-trait", 2000);

        boardSize = config.getInt("hiring.candidates", 4);
        boardRefresh = hours(config.getDouble("hiring.refresh-hours", 12));
        reroll = config.getLong("hiring.reroll", 2000);
        boardOdds = odds(config, "hiring");
        boardPrices = prices(config.getConfigurationSection("hiring.prices"));

        final Map<Integer, Milestone> levels = new TreeMap<>();
        final ConfigurationSection milestoneSection = config.getConfigurationSection("milestones");
        if (milestoneSection != null) {
            for (String level : milestoneSection.getKeys(false)) {
                final ConfigurationSection section = milestoneSection.getConfigurationSection(level);
                if (section == null) {
                    continue;
                }
                try {
                    levels.put(Integer.parseInt(level), new Milestone(section.getString("profession", ANY),
                            SettlerRarity.valueOf(section.getString("rarity", "common").toUpperCase(Locale.ROOT))));
                } catch (IllegalArgumentException exception) {
                    log.warn("Milestone '{}' in settlers.yml has an unknown level or rarity", level).submit();
                }
            }
        }
        milestones = levels;
    }

    private static @NotNull SettlerOdds odds(@NotNull ExtendedYamlConfiguration config, @NotNull String section) {
        final Map<SettlerRarity, Double> rarities = new EnumMap<>(SettlerRarity.class);
        final ConfigurationSection raritySection = config.getConfigurationSection(section + ".rarity-odds");
        if (raritySection != null) {
            for (SettlerRarity rarity : SettlerRarity.values()) {
                rarities.put(rarity, raritySection.getDouble(rarity.name().toLowerCase(Locale.ROOT), 0));
            }
        }
        final Map<String, Double> professions = new LinkedHashMap<>();
        final ConfigurationSection professionSection = config.getConfigurationSection(section + ".profession-odds");
        if (professionSection != null) {
            professionSection.getKeys(false).forEach(profession ->
                    professions.put(profession, professionSection.getDouble(profession)));
        }
        return new SettlerOdds(rarities, professions);
    }

    private static @NotNull Map<SettlerRarity, Long> prices(@Nullable ConfigurationSection section) {
        final Map<SettlerRarity, Long> prices = new EnumMap<>(SettlerRarity.class);
        if (section != null) {
            for (SettlerRarity rarity : SettlerRarity.values()) {
                prices.put(rarity, section.getLong(rarity.name().toLowerCase(Locale.ROOT), 0));
            }
        }
        return prices;
    }

    private static @NotNull Duration hours(double hours) {
        return Duration.ofMinutes((long) (hours * 60));
    }

    /** The settler a clan is sent at one level. */
    @Value
    public static class Milestone {
        /** A profession id, {@link #ANY} or "none". */
        String profession;
        SettlerRarity rarity;
    }
}
