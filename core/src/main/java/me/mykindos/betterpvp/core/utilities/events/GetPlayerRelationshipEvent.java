package me.mykindos.betterpvp.core.utilities.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public class GetPlayerRelationshipEvent extends CustomEvent {

    private final Player player;
    private final Player target;
    @Setter
    private EntityProperty entityProperty = EntityProperty.ALL;


    public GetPlayerRelationshipEvent(Player player, Player target) {
        this.player = player;
        this.target = target;
    }
}
