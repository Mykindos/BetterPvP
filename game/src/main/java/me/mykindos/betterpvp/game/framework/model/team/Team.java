package me.mykindos.betterpvp.game.framework.model.team;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Instance of a team
 */
@Value
@EqualsAndHashCode
public class Team implements ForwardingAudience {

    TeamProperties properties;
    @EqualsAndHashCode.Exclude Set<Participant> participants;

    public Set<Player> getPlayers() {
        return participants.stream().map(Participant::getPlayer).collect(Collectors.toSet());
    }

    @Override
    public @NotNull Iterable<? extends Player> audiences() {
        return participants.stream().map(Participant::getPlayer).collect(Collectors.toSet());
    }
}
