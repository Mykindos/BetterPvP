package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.fishing.data.BiggestFishLeaderboard;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingCountLeaderboard;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingWeightLeaderboard;
import me.mykindos.betterpvp.progression.tree.fishing.model.BaitType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Singleton
@Getter
public class Fishing extends ProgressionTree {

    @Inject
    private FishingRepository statsRepository;

    @Inject
    private FishingWeightLeaderboard weightLeaderboard;

    @Inject
    private FishingCountLeaderboard countLeaderboard;

    @Inject
    private BiggestFishLeaderboard biggestFishLeaderboard;

    @Override
    public @NotNull String getName() {
        return "Fishing";
    }

    public WeighedList<FishingLootType> getLootTypes() {
        return statsRepository.getLootTypes();
    }

    public Set<FishingRodType> getRodTypes() {
        return Collections.unmodifiableSet(statsRepository.getRodTypes());
    }

    public Set<BaitType> getBaitTypes() {
        return Collections.unmodifiableSet(statsRepository.getBaitTypes());
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
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        enabled = config.getOrSaveBoolean("fishing.enabled", true);
        statsRepository.loadConfig(config);
    }
}
