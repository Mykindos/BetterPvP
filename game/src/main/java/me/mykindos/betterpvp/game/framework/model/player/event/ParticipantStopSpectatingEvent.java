package me.mykindos.betterpvp.game.framework.model.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Represents a {@link Participant} no longer spectating a game.
 * <p>
 * After this event is called, the {@link Participant} will be considered alive, will now be able
 * to interact with the game and will be put back into their original game mode.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantStopSpectatingEvent extends ParticipantEvent {

    Player player;
    Participant participant;

}
