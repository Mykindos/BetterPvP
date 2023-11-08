package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingCountLeaderboard;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingWeightLeaderboard;
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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
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
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        statsRepository.loadConfig(config);
    }
}
