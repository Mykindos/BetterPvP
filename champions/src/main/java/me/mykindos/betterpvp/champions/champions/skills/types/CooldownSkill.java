package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;

public interface CooldownSkill extends IChampionsSkill {

    /**
     * This method's purpose is to return the cooldown duration in seconds for the given skill level.
     */
    double getCooldown(int level);

    /**
     * This method's purpose is to indicate whether the player should be informed when the cooldown has finished.
     * "Informed" means that the player will be sent a notification through chat.
     */
    default boolean showCooldownFinished() {
        return true;
    }

    /**
     * Whether this cooldown can be cancelled by certain actions.
     */
    default boolean isCancellable(){
        return false;
    }

    /**
     * Whether to display the cooldown in the action bar or not.
     */
    default boolean shouldDisplayActionBar(Gamer gamer) {
        return isHolding(gamer.getPlayer());
    }

    /**
     * This method's purpose is to provide a priority for cooldown display and handling.
     * I think lower priority values indicate higher importance.
     */
    default int getPriority() {
        return 1000;
    }

    /**
     * This method's purpose is to mark the skill as using a delayed cooldown flow.
     * <p>
     * If true, then the skill will not start its cooldown immediately upon use. Instead, it will rely on the
     * {@link #isUsingSkill(Player)} method to determine when to start the cooldown.
     */
    default boolean isDelayedSkill() {
        return false;
    }

    /**
     * This method's purpose is to let a delayed-skill implementation report whether the player is still actively using
     * it. If this method returns false (at any point), the cooldown will start.
     */
    default boolean isUsingSkill(Player player) {
        return false;
    }

}
