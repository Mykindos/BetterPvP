package me.mykindos.betterpvp.core.combat.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class CustomEntityVelocityEvent extends CustomCancellableEvent {

    private final Entity entity;

    @Setter
    private Vector vector;

}
