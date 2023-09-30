package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingLeaderboard;
import me.mykindos.betterpvp.progression.tree.fishing.model.BaitType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import me.mykindos.betterpvp.progression.tree.fishing.rod.SimpleFishingRod;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
public class Fishing implements ProgressionTree {

    @Getter
    private final FishingRepository repository;
    private final FishingLeaderboard leaderboard = new FishingLeaderboard();

    @Inject
    public Fishing(Progression progression) {
        this.repository = new FishingRepository(progression, this);
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    @Override
    public FishingLeaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public FishingRepository getStatsRepository() {
        return repository;
    }

    public WeighedList<FishingLootType> getLootTypes() {
        return repository.getLootTypes();
    }

    public Set<FishingRodType> getRodTypes() {
        return Collections.unmodifiableSet(repository.getRodTypes());
    }

    public Set<BaitType> getBaitTypes() {
        return Collections.unmodifiableSet(repository.getBaitTypes());
    }

    public Optional<FishingRodType> getRodType(ItemStack itemStack) {
        if (itemStack == null || !itemStack.getType().equals(Material.FISHING_ROD)) {
            return Optional.empty();
        }

        final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ProgressionNamespacedKeys.FISHING_ROD_TYPE)) {
            return Optional.of(SimpleFishingRod.WOODEN); // Default is wooden
        }

        final int rodId = Objects.requireNonNull(pdc.get(ProgressionNamespacedKeys.FISHING_ROD_TYPE, PersistentDataType.INTEGER));
        return getRodTypes().stream()
                .filter(rodType -> rodType.getId() == rodId)
                .findFirst();
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

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        repository.loadConfig(config);
    }
}
