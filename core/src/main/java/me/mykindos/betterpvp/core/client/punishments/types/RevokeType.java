package me.mykindos.betterpvp.core.client.punishments.types;

public enum RevokeType {
    /**
     * Indicates this punishment was in error in some way and should be removed completely
     */
    INCORRECT,
    /**
     * Indicates this punishment was valid, but is being reduced/terminated earlier
     */
    APPEAL
}
