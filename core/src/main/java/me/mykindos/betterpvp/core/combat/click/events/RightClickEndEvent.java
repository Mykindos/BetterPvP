package me.mykindos.betterpvp.core.combat.click.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class RightClickEndEvent extends CustomEvent {

    private final Player player;

}
