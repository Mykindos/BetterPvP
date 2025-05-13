package me.mykindos.betterpvp.game.framework.model.player;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Participant {

    @EqualsAndHashCode.Include private final Player player;
    private final Client client;
    boolean spectating = false;
    /**
     * <p>{@code true} if set to spectate the next game (i.e. a permanent spectator)</p>
     * <p>{@code false} if not set to spectate the next game (a temporary spectator, valid for balancing in some instances)</p>
     */
    boolean spectateNextGame = false;
    boolean alive = true;

    public boolean isAlive() {
        return !spectating && alive;
    }

    public boolean isSpectating() {
        return spectating;
    }

}
