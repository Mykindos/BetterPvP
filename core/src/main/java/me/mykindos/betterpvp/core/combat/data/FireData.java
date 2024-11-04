package me.mykindos.betterpvp.core.combat.data;

import lombok.Data;
import org.bukkit.entity.LivingEntity;

@Data
public class FireData {
    private final LivingEntity damager;
    private final long start;
    private final long duration;

    public FireData(LivingEntity damager, long duration) {
        this.damager = damager;
        this.start = System.currentTimeMillis();
        this.duration = duration;
    }
}
