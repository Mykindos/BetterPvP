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
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    /** One profession's working cap. */
    @Value
    public static class WorkingCap {
        List<Integer> byHallStage;
        /** How many to add for each stage of a finished structure, by structure id. */
        Map<String, Integer> perStageOf;
    }
}
