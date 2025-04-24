package me.mykindos.betterpvp.game.framework.listener.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Called when a {@link Participant} is put back into the game after being eliminated.
 * <p>
 * This event cannot be canceled. The player will always respawn. To cancel the preparation of the respawn, use {@link ParticipantPreRespawnEvent}.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantRespawnEvent extends CustomEvent {

    Participant participant;
    Player player;

}
