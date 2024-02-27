package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import me.mykindos.betterpvp.clans.clans.vault.blacklist.perk.VaultRestriction;

public class ClassBlacklist extends BlacklistEntry<Class<?>> {

    protected ClassBlacklist(Class<?> clazz, VaultRestriction bypass) {
        super(clazz, bypass);
    }

    @Override
    protected boolean matches(Object object) {
        return object != null && value == object.getClass();
    }

}
