package me.mykindos.betterpvp.progression.model;

import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import org.bukkit.entity.Player;

import java.util.List;

public interface ProgressionPerk {

    String getName();

    List<String> getDescription(Player player, ProgressionData<?> data);

    Class<? extends ProgressionTree>[] acceptedTrees();

    boolean canUse(Player player, ProgressionData<?> data);


}
