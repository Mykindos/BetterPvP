package me.mykindos.betterpvp.progression.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import org.bukkit.entity.Player;

@Getter
public class PlayerProgressionExperienceEvent extends CustomEvent {

    private final ProgressionTree tree;
    private final Player player;
    private final long gainedExp;
    private final ProgressionData<?> data;
    private final int level;
    private final int previousLevel;
    private final boolean levelUp;

    public PlayerProgressionExperienceEvent(ProgressionTree tree, Player player, long gainedExp, ProgressionData<?> data, int level, int previousLevel) {
        this.tree = tree;
        this.player = player;
        this.gainedExp = gainedExp;
        this.data = data;
        this.level = level;
        this.previousLevel = previousLevel;
        this.levelUp = level != previousLevel;
    }
}
