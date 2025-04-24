package me.mykindos.betterpvp.game.framework.module.powerup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantPowerupEvent extends CustomCancellableEvent {
    Player player;
    Participant participant;
    Powerup powerup;
}
