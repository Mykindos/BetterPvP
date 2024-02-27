package me.mykindos.betterpvp.clans.clans.vault.blacklist;

import me.mykindos.betterpvp.clans.clans.vault.blacklist.perk.VaultRestriction;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class TypeBlacklist extends BlacklistEntry<Material> {

    protected TypeBlacklist(Material material, @Nullable VaultRestriction bypass) {
        super(material, bypass);
    }

    @Override
    protected boolean matches(Object object) {
        if (object instanceof Material) {
            return object.equals(value);
        }

        return false;
    }

}
