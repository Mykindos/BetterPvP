package me.mykindos.betterpvp.core.combat.damagelog;

import lombok.Data;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nullable;

@Data
public class DamageLog {

    @Nullable
    private final LivingEntity damager;

    private final EntityDamageEvent.DamageCause damageCause;
    private final double damage;
    private final String[] reason;
    private final long expiry = System.currentTimeMillis() + 10000;


}
