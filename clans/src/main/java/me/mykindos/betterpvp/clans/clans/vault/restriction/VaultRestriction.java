package me.mykindos.betterpvp.clans.clans.vault.restriction;

import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents an item restriction that can be applied to a vault
 */
public abstract class VaultRestriction {

    protected final Map<ClanPerk, Integer> allowedPerks = new HashMap<>();
    protected final Integer allowedCount;

    protected VaultRestriction(int allowedCount) {
        this.allowedCount = allowedCount;
    }

    protected VaultRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks) {
        this.allowedCount = null;
        this.allowedPerks.putAll(allowedPerks);
    }

    public abstract boolean matches(@NotNull ItemStack itemStack);

    /**
     * @param vault The vault to check
     * @return The amount of items allowed in the vault. An empty {@link Optional} allows unlimited items.
     */
    public final OptionalInt getRemainingCount(ClanVault vault) {
        final int existing = UtilInventory.getCount(vault.getContents().values().toArray(new ItemStack[0]), this::matches);
        if (allowedCount != null) {
            return OptionalInt.of(Math.max(0, allowedCount - existing)); // Prioritize static count
        }

        final HashSet<ClanPerk> owned = new HashSet<>(allowedPerks.keySet());
        owned.retainAll(ClanPerkManager.getInstance().getPerks(vault.getClan()));
        if (owned.isEmpty()) {
            return OptionalInt.of(0); // Disallowed
        }

        // Return the first negative perk or the highest value
        int allowed = 0;
        for (ClanPerk perk : owned) {
            final int value = allowedPerks.get(perk);
            if (value < 0) {
                return OptionalInt.empty(); // Infinite
            }

            allowed = Math.max(allowed, value);
        }

        // Return the difference
        return OptionalInt.of(Math.max(0, allowed - existing));
    }

}
