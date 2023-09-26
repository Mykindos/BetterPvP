package me.mykindos.betterpvp.progression.tree.fishing;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishType;
import me.mykindos.betterpvp.progression.tree.fishing.model.FishingRodType;
import me.mykindos.betterpvp.progression.tree.fishing.repository.FishingRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class Fishing implements ProgressionTree {

    private final FishingRepository repository;

    @Inject
    public Fishing(@NotNull final FishingRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getName() {
        return "Fishing";
    }

    public Set<FishType> getFishTypes() {
        return Collections.unmodifiableSet(repository.getFishTypes());
    }

    public Set<FishingRodType> getRodTypes() {
        return Collections.unmodifiableSet(repository.getRodTypes());
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        repository.loadConfig(config);
        getFishTypes().forEach(fishType -> fishType.loadConfig(config));
        getRodTypes().forEach(rodType -> rodType.loadConfig(config));
    }
}
