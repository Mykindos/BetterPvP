package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import me.mykindos.betterpvp.clans.clans.vault.blacklist.perk.VaultRestriction;
import org.jetbrains.annotations.Nullable;

public class ModelBlacklist extends BlacklistEntry<Integer> {

    protected ModelBlacklist(int model, @Nullable VaultRestriction bypass) {
        super(model, bypass);
    }

    @Override
    protected boolean matches(Object object) {
        return object instanceof Integer && ((int) object) == value;
    }

}
