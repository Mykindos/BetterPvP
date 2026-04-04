package me.mykindos.betterpvp.core.combat.events;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerCombatFeatureStateChangeEvent extends CustomEvent {

    Player player;
    boolean previousActive;
    boolean active;
}
