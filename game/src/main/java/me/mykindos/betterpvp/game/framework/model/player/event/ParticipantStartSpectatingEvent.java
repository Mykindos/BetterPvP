package me.mykindos.betterpvp.game.framework.model.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Represents a {@link Participant} spectating a game.
 * <p>
 * After this event is called, the {@link Participant} will be no longer be considered alive, won't be able
 * to interact with the game and will be put in spectator mode.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantStartSpectatingEvent extends ParticipantEvent {

    Player player;
    Participant participant;

}
