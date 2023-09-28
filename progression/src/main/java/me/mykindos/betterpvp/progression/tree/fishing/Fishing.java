package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.model.Leaderboard;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.fishing.data.FishingLeaderboard;
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
import java.util.Optional;
import java.util.Set;

public class Fishing implements ProgressionTree {

    private final FishingRepository repository;
    private final FishingLeaderboard leaderboard = new FishingLeaderboard();

    @Inject
    public Fishing(@NotNull final FishingRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    @Override
    public <T extends ProgressionTree> Leaderboard<T> getLeaderboard() {
        return null;
    }

    public WeighedList<FishingLootType> getLootTypes() {
        return repository.getLootTypes();
    }

    public Set<FishingRodType> getRodTypes() {
        return Collections.unmodifiableSet(repository.getRodTypes());
    }

    public Optional<FishingRodType> getRodType(ItemStack itemStack) {
        if (itemStack == null || !itemStack.getType().equals(Material.FISHING_ROD)) {
            return Optional.empty();
        }

        final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ProgressionNamespacedKeys.FISHING_ROD_TYPE)) {
            return Optional.of(SimpleFishingRod.WOODEN); // Default is wooden
        }

        final int rodId = pdc.get(ProgressionNamespacedKeys.FISHING_ROD_TYPE, PersistentDataType.INTEGER);
        return getRodTypes().stream()
                .filter(rodType -> rodType.getId() == rodId)
                .findFirst();
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        repository.loadConfig(config);
    }
}
