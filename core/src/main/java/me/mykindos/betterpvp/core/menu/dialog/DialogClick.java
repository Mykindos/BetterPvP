package me.mykindos.betterpvp.core.menu.dialog;

import org.bukkit.entity.Player;

/**
 * Runs when a player clicks a region or button of an open dialog screen.
 */
@FunctionalInterface
public interface DialogClick {

    void onClick(Player player, DialogInputs inputs);
}
