package me.mykindos.betterpvp.game.framework.model.team;

import lombok.Value;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Instance of a team
 */
@Value
public class Team {

    TeamProperties properties;
    Set<Player> players;

}
