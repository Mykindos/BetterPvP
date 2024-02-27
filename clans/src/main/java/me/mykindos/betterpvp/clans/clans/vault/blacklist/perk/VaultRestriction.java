package me.mykindos.betterpvp.clans.clans.vault.blacklist.perk;

import me.mykindos.betterpvp.clans.clans.vault.ClanVault;

/**
 * Represents an item restriction that can be applied to a vault
 */
public interface VaultRestriction {

    /**
     * @param vault The vault to check
     * @return The amount of items allowed in the vault. Anything less than 0 is considered unlimited.
     */
    int getAllowedCount(ClanVault vault);

}
