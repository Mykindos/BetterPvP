package me.mykindos.betterpvp.progression.tree.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.tree.mining.data.MiningOresMinedLeaderboard;
import me.mykindos.betterpvp.progression.tree.mining.repository.MiningRepository;
import org.jetbrains.annotations.NotNull;

@Singleton
@Getter
public class Mining extends ProgressionTree {

    @Inject
    private MiningRepository statsRepository;

    @Inject
    private MiningService miningService;

    @Inject
    private MiningOresMinedLeaderboard leaderboard;

    @Override
    public @NotNull String getName() {
        return "Mining";
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        statsRepository.loadConfig(config);
        leaderboard.forceUpdate(); // Force update because mining service reloads tracked blocks
    }

}
