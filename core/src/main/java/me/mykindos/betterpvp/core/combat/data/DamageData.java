package me.mykindos.betterpvp.core.combat.data;

import lombok.Data;
import org.bukkit.event.entity.EntityDamageEvent;

@Data
public class DamageData {

    private final String uuid;
    private final EntityDamageEvent.DamageCause cause;
    private final String damager;
    private final long damageDelay;
    private final long timeOfDamage = System.currentTimeMillis();

}
