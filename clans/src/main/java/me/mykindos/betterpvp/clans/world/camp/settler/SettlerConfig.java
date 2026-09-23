package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.crew.CrewLimits;
import me.mykindos.betterpvp.core.world.settler.wage.FixedWageModel;
import me.mykindos.betterpvp.core.world.settler.wage.IdleWorkingWageModel;
import me.mykindos.betterpvp.core.world.settler.wage.WageModel;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The numbers settlers run on, read from {@code settlers.yml}. */
@Singleton
@CustomLog
public class SettlerConfig implements Reloadable {

    private final Clans clans;

    /** What new settlers are rolled from. */
    @Getter
    private SettlerTable table;
    private List<Integer> population = List.of();
    private final Map<String, WorkingCap> workingCaps = new HashMap<>();
    private final Map<String, Map<String, Object>> looks = new HashMap<>();
    private final Map<SettlerRarity, BuilderNumbers> builders = new EnumMap<>(SettlerRarity.class);
    private final Map<String, Trade> trades = new HashMap<>();
    private final Map<String, Map<String, Double>> traitNumbers = new HashMap<>();
    /** Limits on one job's crew. */
    @Getter
    private CrewLimits crewLimits = CrewLimits.NONE;
    /** What every new camp starts with. */
    @Getter
    private List<Starter> startingSettlers = List.of();
    /** How settlers are paid. */
    @Getter
    private WageModel wageModel = new FixedWageModel(Map.of());
    /** How long a settler strikes before it leaves. */
    @Getter
    private Duration strikeLimit = Duration.ofHours(72);

    @Inject
    public SettlerConfig(@NotNull Clans clans) {
        this.clans = clans;
        clans.getReloadables().add(this);
        reload();
    }

    @Override
    public void reload() {
        final ExtendedYamlConfiguration config = clans.getConfig("settlers");

        final Map<SettlerRarity, RarityNumbers> rarities = new EnumMap<>(SettlerRarity.class);
        for (SettlerRarity rarity : SettlerRarity.values()) {
            final ConfigurationSection section = config.getConfigurationSection(
                    "rarities." + rarity.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                log.warn("Settler rarity '{}' is missing from settlers.yml", rarity).submit();
                continue;
            }
            rarities.put(rarity, new RarityNumbers(section.getInt("traits", 1), section.getDouble("trait-strength", 1),
                    section.getDouble("stats", 1), section.getDouble("trade-off-chance", 0)));
        }

        final Map<String, List<String>> histories = new LinkedHashMap<>();
        final ConfigurationSection historySection = config.getConfigurationSection("histories");
        if (historySection != null) {
            historySection.getKeys(false).forEach(source ->
                    histories.put(source, List.copyOf(historySection.getStringList(source))));
        }

        table = new SettlerTable(rarities, List.copyOf(config.getStringList("names.first")),
                List.copyOf(config.getStringList("names.bynames")), histories);
        population = List.copyOf(config.getIntegerList("population.by-hall-stage"));

        looks.clear();
        final ConfigurationSection lookSection = config.getConfigurationSection("looks");
        if (lookSection != null) {
            for (String profession : lookSection.getKeys(false)) {
                final ConfigurationSection look = lookSection.getConfigurationSection(profession);
                if (look == null) {
                    continue;
                }
                looks.put(profession, values(look));
                final ConfigurationSection rarityLooks = look.getConfigurationSection("rarities");
                if (rarityLooks != null) {
                    for (String rarity : rarityLooks.getKeys(false)) {
                        final ConfigurationSection byRarity = rarityLooks.getConfigurationSection(rarity);
                        if (byRarity != null) {
                            looks.put(profession + "." + rarity.toLowerCase(Locale.ROOT), values(byRarity));
                        }
                    }
                }
            }
        }

        builders.clear();
        for (SettlerRarity rarity : SettlerRarity.values()) {
            final ConfigurationSection section = config.getConfigurationSection(
                    "builders." + rarity.name().toLowerCase(Locale.ROOT));
            if (section != null) {
                builders.put(rarity, new BuilderNumbers(section.getInt("workforce", 1), section.getDouble("speed", 1),
                        section.getDouble("efficiency", 0.5)));
            }
        }

        trades.clear();
        final ConfigurationSection tradeSection = config.getConfigurationSection("trades");
        if (tradeSection != null) {
            for (String trade : tradeSection.getKeys(false)) {
                final ConfigurationSection section = tradeSection.getConfigurationSection(trade);
                if (section != null) {
                    trades.put(trade, new Trade(section.getString("resource"), section.getDouble("specialty", 0),
                            section.getInt("workforce", 0), Set.copyOf(section.getStringList("compatible"))));
                }
            }
        }

        final Map<SettlerRarity, Integer> perRarity = new EnumMap<>(SettlerRarity.class);
        final ConfigurationSection rarityLimits = config.getConfigurationSection("crews.per-rarity");
        if (rarityLimits != null) {
            for (SettlerRarity rarity : SettlerRarity.values()) {
                final String name = rarity.name().toLowerCase(Locale.ROOT);
                if (rarityLimits.contains(name)) {
                    perRarity.put(rarity, rarityLimits.getInt(name));
                }
            }
        }
        crewLimits = new CrewLimits(config.getInt("crews.max-size", 5), config.getDouble("crews.max-speed", 4),
                Map.copyOf(perRarity), config.getDouble("crews.compatible-bonus", 0.1));

        traitNumbers.clear();
        final ConfigurationSection traitSection = config.getConfigurationSection("traits");
        if (traitSection != null) {
            for (String trait : traitSection.getKeys(false)) {
                final ConfigurationSection section = traitSection.getConfigurationSection(trait);
                if (section == null) {
                    continue;
                }
                final Map<String, Double> numbers = new HashMap<>();
                section.getKeys(false).forEach(key -> numbers.put(key, section.getDouble(key)));
                traitNumbers.put(trait, numbers);
            }
        }

        final List<Starter> starters = new ArrayList<>();
        for (Map<?, ?> starter : config.getMapList("starting-settlers")) {
            final Object profession = starter.get("profession");
            final Object rarity = starter.get("rarity");
            try {
                starters.add(new Starter(profession == null || "none".equals(profession) ? null : profession.toString(),
                        SettlerRarity.valueOf(String.valueOf(rarity == null ? "common" : rarity).toUpperCase(Locale.ROOT))));
            } catch (IllegalArgumentException exception) {
                log.warn("Unknown rarity '{}' in settlers.yml starting-settlers", rarity).submit();
            }
        }
        startingSettlers = List.copyOf(starters);

        strikeLimit = Duration.ofMinutes((long) (config.getDouble("wages.strike-hours", 72) * 60));
        final String model = config.getString("wages.model", "fixed");
        if ("idle-working".equalsIgnoreCase(model)) {
            wageModel = new IdleWorkingWageModel(rates(config.getConfigurationSection("wages.idle-working.idle")),
                    rates(config.getConfigurationSection("wages.idle-working.working")));
        } else {
            if (!"fixed".equalsIgnoreCase(model)) {
                log.warn("Unknown wage model '{}' in settlers.yml, paying fixed wages", model).submit();
            }
            wageModel = new FixedWageModel(rates(config.getConfigurationSection("wages.fixed")));
        }

        workingCaps.clear();
        final ConfigurationSection professions = config.getConfigurationSection("professions");
        if (professions != null) {
            for (String profession : professions.getKeys(false)) {
                final Map<String, Integer> perStage = new HashMap<>();
                final ConfigurationSection bonus = professions.getConfigurationSection(profession + ".per-stage-of");
                if (bonus != null) {
                    bonus.getKeys(false).forEach(structure -> perStage.put(structure, bonus.getInt(structure)));
                }
                workingCaps.put(profession, new WorkingCap(
                        List.copyOf(professions.getIntegerList(profession + ".by-hall-stage")), perStage));
            }
        }
    }

    /** Coins an hour by profession and rarity, from a {@code profession: {rarity: coins}} section. */
    private static @NotNull Map<String, Map<SettlerRarity, Double>> rates(@Nullable ConfigurationSection section) {
        final Map<String, Map<SettlerRarity, Double>> rates = new HashMap<>();
        if (section == null) {
            return rates;
        }
        for (String profession : section.getKeys(false)) {
            final Map<SettlerRarity, Double> byRarity = new EnumMap<>(SettlerRarity.class);
            for (SettlerRarity rarity : SettlerRarity.values()) {
                final String path = profession + "." + rarity.name().toLowerCase(Locale.ROOT);
                if (section.contains(path)) {
                    byRarity.put(rarity, section.getDouble(path));
                }
            }
            rates.put(profession, byRarity);
        }
        return rates;
    }

    private static @NotNull Map<String, Object> values(@NotNull ConfigurationSection section) {
        final Map<String, Object> values = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                values.put(key, section.get(key));
            }
        }
        return values;
    }

    /** The default look. */
    public @NotNull SettlerLook defaultLook() {
        return look(List.of("default"));
    }

    /**
     * The look for a settler of {@code profession} (null for none) and {@code rarity}: the default, overridden by the
     * profession's, overridden by that profession's for the rarity.
     */
    public @NotNull SettlerLook look(@Nullable String profession, @NotNull SettlerRarity rarity) {
        final String id = profession == null ? "none" : profession;
        return look(List.of("default", id, id + "." + rarity.name().toLowerCase(Locale.ROOT)));
    }

    /** A look named in settlers.yml, such as the Steward's, over the default. */
    public @NotNull SettlerLook look(@NotNull String id) {
        return look(List.of("default", id));
    }

    private @NotNull SettlerLook look(@NotNull List<String> layers) {
        final Map<String, Object> merged = new HashMap<>();
        layers.forEach(layer -> merged.putAll(looks.getOrDefault(layer, Map.of())));
        final Object size = merged.get("size");
        final Object skin = merged.get("skin");
        return new SettlerLook(String.valueOf(merged.getOrDefault("model", "scene_market_1")),
                skin == null ? null : skin.toString(),
                String.valueOf(merged.getOrDefault("idle", "idle")),
                String.valueOf(merged.getOrDefault("walk", "walk")),
                String.valueOf(merged.getOrDefault("work", "idle")),
                size instanceof Number number ? number.doubleValue() : 1.0);
    }

    /** What a Builder of {@code rarity} brings before its trade and traits. */
    public @NotNull BuilderNumbers builder(@NotNull SettlerRarity rarity) {
        return builders.getOrDefault(rarity, new BuilderNumbers(1, 1, 0.5));
    }

    public @NotNull Optional<Trade> trade(@Nullable String trade) {
        return trade == null ? Optional.empty() : Optional.ofNullable(trades.get(trade));
    }

    /** One of {@code trait}'s numbers at common strength, or {@code fallback} if settlers.yml does not set it. */
    public double trait(@NotNull String trait, @NotNull String number, double fallback) {
        return traitNumbers.getOrDefault(trait, Map.of()).getOrDefault(number, fallback);
    }

    /** How many settlers a camp can have with its Great Hall at {@code hallStage}, or -1 for no Great Hall. */
    public int population(int hallStage) {
        return byStage(population, hallStage);
    }

    /** How many of {@code profession} can work at once, if it has a cap of its own. */
    public @NotNull Optional<WorkingCap> workingCap(@NotNull String profession) {
        return Optional.ofNullable(workingCaps.get(profession));
    }

    /** The value for {@code stage} in a list that follows the Great Hall, where later stages keep the last value. */
    static int byStage(@NotNull List<Integer> values, int stage) {
        if (stage < 0 || values.isEmpty()) {
            return 0;
        }
        return values.get(Math.min(stage, values.size() - 1));
    }

    /** What a Builder of one rarity brings before its trade and traits. */
    @Value
    public static class BuilderNumbers {
        int workforce;
        double speed;
        double efficiency;
    }

    /** One Builder trade. */
    @Value
    public static class Trade {
        /** The resource it specialises in, or null for none. */
        @Nullable String resource;
        /** Extra speed, as a share, on jobs whose cost is mostly that resource. */
        double specialty;
        int workforce;
        Set<String> compatible;
    }

    /** One settler every new camp starts with. */
    @Value
    public static class Starter {
        @Nullable String profession;
        SettlerRarity rarity;
    }

    /** One profession's working cap. */
    @Value
    public static class WorkingCap {
        List<Integer> byHallStage;
        /** How many to add for each stage of a finished structure, by structure id. */
        Map<String, Integer> perStageOf;
    }
}
