package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

/**
 * Represents the different phases of the Vanguards Might ability.
 */
public enum VanguardsMightAbilityPhase  {

    /**
     * Player is channeling the skill and absorbing damage to charge the ability.
     */
    CHANNELING,

    /**
     * Player is in the transference phase after channeling, where they are transferring charge into a strength effect.
     */
    TRANSFERENCE,

    /**
     * Player has gained a strength effect after the ability charge has been transferred.
     */
    STRENGTH_EFFECT
}
