package me.mykindos.betterpvp.champions.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerCheckShieldEvent extends CustomEvent {

    private final Player player;

    @Setter
    private boolean shouldHaveShield;

}
