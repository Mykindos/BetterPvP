package me.mykindos.betterpvp.game.framework.model.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Represents a {@link Participant} alive status change to false, indicating the player has died, conceptually.
 * <p>
 * This does not necessarily mean the player actually died, but rather that they are no longer considered alive.
 * After this event is called, the player is considered dead, teleported to a spawn point, their inventory is cleared,
 * and they are no longer able to interact with the game, spectating until revived, if at all.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantDeathEvent extends ParticipantEvent {

    Player player;
    Participant participant;

}
