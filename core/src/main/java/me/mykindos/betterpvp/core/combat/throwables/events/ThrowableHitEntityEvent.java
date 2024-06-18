package me.mykindos.betterpvp.core.combat.throwables.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ThrowableHitEntityEvent extends ThrowableHitEvent {

    private LivingEntity collision;

    public ThrowableHitEntityEvent(ThrowableItem throwable, LivingEntity livingEntity) {
        super(throwable);
        this.collision = livingEntity;
    }
}
