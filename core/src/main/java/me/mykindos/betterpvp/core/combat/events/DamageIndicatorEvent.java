package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


@EqualsAndHashCode(callSuper = true)
@Data
public class DamageIndicatorEvent extends CustomCancellableEvent {

    private final Player player;
    private final Entity entity;
    private final double damage;

}
