package me.mykindos.betterpvp.clans.world.camp.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The upgrades camp structures offer. Each upgrade's effect is its own class, which declares the upgrade here when it
 * is created, so a structure only ever offers upgrades that do something. Their numbers come from {@code camps.yml}.
 * An upgrade that is used rather than always on also gives the page it opens.
 */
@Singleton
public class CampUpgrades {

    private final CampStore store;
    private final Map<String, List<Declared>> declared = new HashMap<>();
    private final Map<String, UpgradePage> pages = new HashMap<>();

    @Inject
    public CampUpgrades(@NotNull CampStore store) {
        this.store = store;
    }

    /**
     * Offers upgrade {@code id} on structure {@code structure} from {@code stage}.
     *
     * @param stage the stage that offers it, 1 being the first
     */
    public void declare(@NotNull String structure, @NotNull String id, int stage) {
        final List<Declared> upgrades = declared.computeIfAbsent(structure, key -> new ArrayList<>());
        upgrades.removeIf(upgrade -> upgrade.getId().equals(id));
        upgrades.add(new Declared(id, stage - 1));
        upgrades.sort(Comparator.comparingInt(Declared::getStage));
    }

    /** Makes a fitted upgrade {@code id} open {@code page} when used. */
    public void page(@NotNull String id, @NotNull UpgradePage page) {
        pages.put(id, page);
    }

    public @NotNull Optional<UpgradePage> page(@NotNull String id) {
        return Optional.ofNullable(pages.get(id));
    }

    public @NotNull List<Declared> declared(@NotNull String structure) {
        return List.copyOf(declared.getOrDefault(structure, List.of()));
    }

    /** Whether a working {@code structure} in camp {@code key} has upgrade {@code id}. */
    public boolean has(@NotNull SiteKey key, @NotNull String structure, @NotNull String id) {
        return store.cached(key.getOwnerId())
                .map(Camp::getHolding)
                .map(holding -> holding.ofType(structure).stream()
                        .anyMatch(placed -> placed.getCondition() == StructureCondition.ACTIVE && placed.hasUpgrade(id)))
                .orElse(false);
    }

    /** An upgrade a structure offers, and the stage that offers it, 0 being the first. */
    @Value
    public static class Declared {
        String id;
        int stage;
    }
}
