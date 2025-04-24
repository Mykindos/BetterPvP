package me.mykindos.betterpvp.game.framework.model.player.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import org.bukkit.entity.Player;

/**
 * Represents a {@link Participant} alive status change to true, indicating the player has revived, conceptually.
 * <p>
 * After this event is called, the player is considered alive, teleported to a spawn point, their inventory is reset,
 * and they are now able to interact with the game.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ParticipantReviveEvent extends ParticipantEvent {

    Player player;
    Participant participant;

}
