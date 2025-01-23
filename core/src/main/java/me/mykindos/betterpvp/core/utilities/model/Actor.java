package me.mykindos.betterpvp.core.utilities.model;

import org.bukkit.entity.Player;

/**
 * Represents an object that has an action attached to it that can be run by a player
 */
public interface Actor {

    void act(Player runner);

}
