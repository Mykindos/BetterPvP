package me.mykindos.betterpvp.progression.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerProgressionExperienceEvent extends CustomEvent {

    private final Player player;
    private final String profession;
    private final double gainedExp;
    private final int level;
    private final int previousLevel;
    private final boolean levelUp;

}
