package me.mykindos.betterpvp.core.combat.combatlog.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.combat.combatlog.CombatLog;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerClickCombatLogEvent extends CustomEvent {

    private final Player player;
    private final CombatLog combatLog;

}
