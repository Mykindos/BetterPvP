package me.mykindos.betterpvp.core.combat.throwables.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
public class ThrowableHitEntityEvent extends ThrowableHitEvent {

    @Getter
    private final LivingEntity collision;

    public ThrowableHitEntityEvent(ThrowableItem throwable, LivingEntity livingEntity) {
        super(throwable);
        this.collision = livingEntity;
    }
}
