package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public abstract class ClanVaultLegend implements ClanPerk {

    private final int count;
    private final int minReq;

    protected ClanVaultLegend(int count, int minReq) {
        this.count = count;
        this.minReq = minReq;
    }

    @Override
    public String getName() {
        return count + " Vault Legends";
    }

    @Override
    public int getMinimumLevel() {
        return minReq;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Component.text(count + " legendaries are allowed in your clan vault.", NamedTextColor.GRAY)
        };
    }

}
