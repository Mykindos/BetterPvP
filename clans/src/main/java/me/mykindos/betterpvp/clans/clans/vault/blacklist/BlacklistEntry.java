package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.clans.clans.vault.blacklist.perk.VaultRestriction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class BlacklistEntry<T> {

    protected final T value;
    protected @NotNull VaultRestriction restriction;

    protected BlacklistEntry(T value, int allowedCount) {
        this.value = value;
        this.restriction = vault -> allowedCount;
    }

    protected BlacklistEntry(T value, @NotNull VaultRestriction restriction) {
        this.value = value;
        this.restriction = restriction;
    }

    public int getAllowedCount(ClanVault vault, Object object) {
        if (object == null) {
            return false;
        }

        if (!matches(object)) {
            return false;
        }

        return restriction.getAllowedCount(vault);
    }

    protected abstract boolean matches(Object object) ;

}
