package me.mykindos.betterpvp.core.combat.damagelog;

import lombok.Data;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;

@Data
public class DamageLog {

    public static final long EXPIRY = 15_000L;

    @Nullable
    private final LivingEntity damager;

    private final DamageCause damageCause;
    private final double damage;
    private final String[] reason;
    private final long time = System.currentTimeMillis();
    private final long expiry = System.currentTimeMillis() + EXPIRY;


}
