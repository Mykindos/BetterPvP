package me.mykindos.betterpvp.game.framework.listener.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Called when a {@link Participant} is ready to respawn. A timer will start after this event is called
 * and the player will respawn when the timer is up.
 * <p>
 * If this event is canceled, the player will not respawn.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantPreRespawnEvent extends CustomCancellableEvent {

    Participant participant;
    Player player;

}
