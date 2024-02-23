package me.mykindos.betterpvp.core.utilities.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Getter
public class GetEntityRelationshipEvent extends CustomEvent {

    private final LivingEntity entity;
    private final LivingEntity target;
    @Setter
    private EntityProperty entityProperty = EntityProperty.ALL;


    public GetEntityRelationshipEvent(LivingEntity entity, LivingEntity target) {
        this.entity = entity;
        this.target = target;
    }
}
