package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import org.bukkit.entity.Player;

public interface ProgressionPerk {

    String getName();

    String[] getDescription(int level);

    Class<? extends ProgressionTree>[] acceptedTrees();

    boolean canUse(Player player, ProgressionData<?> data);


}
