package me.mykindos.betterpvp.core.components.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClanAddExperienceEvent extends CustomEvent {

    private final Player player;
    private final long experience;

}
