package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import me.mykindos.betterpvp.clans.clans.vault.blacklist.perk.VaultRestriction;
import org.jetbrains.annotations.Nullable;

public class SubclassBlacklist extends BlacklistEntry<Class<?>> {

    protected SubclassBlacklist(Class<?> clazz, @Nullable VaultRestriction bypass) {
        super(clazz, bypass);
    }

    @Override
    protected boolean matches(Object object) {
        return object != null && value.isAssignableFrom(object.getClass());
    }

}
