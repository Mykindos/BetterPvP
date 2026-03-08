package me.mykindos.betterpvp.core.interaction.actor;

import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Abstracts Player and Entity for unified interaction handling.
 * Allows interactions to work with both players and non-player entities.
 */
public interface InteractionActor {

    /**
     * Get the unique ID of this actor.
     *
     * @return the unique ID
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Get the underlying living entity.
     *
     * @return the living entity
     */
    @NotNull
    LivingEntity getEntity();

    /**
     * Get the current location of this actor.
     *
     * @return the current location
     */
    @NotNull
    Location getLocation();

    /**
     * Get the client associated with this actor, if it's a player.
     *
     * @return the client, or null if this actor is not a player
     */
    @Nullable
    Client getClient();

    /**
     * Check if this actor is a player.
     *
     * @return true if this actor is a player
     */
    boolean isPlayer();

    /**
     * Check if this actor is still valid (exists and alive).
     *
     * @return true if the actor is valid
     */
    boolean isValid();

    // State checks

    /**
     * Check if the actor is currently sneaking.
     *
     * @return true if sneaking
     */
    boolean isSneaking();

    /**
     * Check if the actor is currently sprinting.
     *
     * @return true if sprinting
     */
    boolean isSprinting();

    /**
     * Check if the actor is on the ground.
     *
     * @return true if on ground
     */
    boolean isOnGround();

    /**
     * Check if the actor is in liquid (water or lava).
     *
     * @return true if in liquid
     */
    boolean isInLiquid();

    // Energy/effects

    /**
     * Check if the actor has at least the specified amount of energy.
     * For non-player entities, this always returns true.
     *
     * @param amount the amount of energy required
     * @return true if the actor has sufficient energy
     */
    boolean hasEnergy(double amount);

    /**
     * Attempt to use energy from the actor.
     *
     * @param name   the name of the ability using energy
     * @param amount the amount of energy to use
     * @param inform whether to inform the actor if they lack energy
     * @return true if the energy was successfully used
     */
    boolean useEnergy(String name, double amount, boolean inform);

    /**
     * Check if the actor is currently silenced (unable to use abilities).
     *
     * @return true if silenced
     */
    boolean isSilenced();

    /**
     * Check if the actor is currently stunned (unable to act).
     *
     * @return true if stunned
     */
    boolean isStunned();
}
