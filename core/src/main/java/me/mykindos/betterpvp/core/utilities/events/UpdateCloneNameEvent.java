package me.mykindos.betterpvp.core.utilities.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Getter
public class UpdateCloneNameEvent extends CustomEvent {

    private final LivingEntity clone;
    private final Player spawner;
    @Setter
    private EntityProperty entityProperty = EntityProperty.ALL;


    public UpdateCloneNameEvent(LivingEntity clone, Player spawner) {
        this.clone = clone;
        this.spawner = spawner;
    }
}
