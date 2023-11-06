package me.mykindos.betterpvp.core.utilities.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public class GetPlayerPropertyEvent extends CustomEvent {

    private final Player original;
    private final Player target;
    private final EntityProperty compareProperty;
    @Setter
    private EntityProperty returnProperty;


    public GetPlayerPropertyEvent(Player original, Player target, EntityProperty compareProperty, EntityProperty returnProperty) {
        this.original = original;
        this.target = target;
        this.compareProperty = compareProperty;
        this.returnProperty = returnProperty;
    }
}
