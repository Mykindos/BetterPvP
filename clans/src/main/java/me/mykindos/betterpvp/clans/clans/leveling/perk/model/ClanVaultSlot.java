package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public abstract class ClanVaultSlot implements ClanPerk {

    private final int slots;
    private final int minReq;

    protected ClanVaultSlot(int slots, int minReq) {
        this.slots = slots;
        this.minReq = minReq;
    }

    @Override
    public String getName() {
        return "+" + slots + " Vault Slots";
    }

    @Override
    public int getMinimumLevel() {
        return minReq;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Component.text("Gain " + slots + " extra slots for your clan vault.", NamedTextColor.GRAY)
        };
    }

}
