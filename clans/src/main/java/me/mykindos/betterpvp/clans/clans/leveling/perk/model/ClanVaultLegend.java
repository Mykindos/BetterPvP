package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public abstract class ClanVaultLegend implements ClanPerk {

    private final int count;
    private final int minReq;

    protected ClanVaultLegend(int slots, int minReq) {
        this.count = slots;
        this.minReq = minReq;
    }

    @Override
    public String getName() {
        return "+" + count + " Vault Slots";
    }

    @Override
    public int getMinimumLevel() {
        return minReq;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Component.text("Gain " + count + " extra slots for your clan vault.", NamedTextColor.GRAY)
        };
    }

}
