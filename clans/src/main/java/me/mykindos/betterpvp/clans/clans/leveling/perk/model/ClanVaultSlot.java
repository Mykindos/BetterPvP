package me.mykindos.betterpvp.clans.clans.leveling.perk.model;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Getter
public class ClanVaultSlot implements ClanPerk {

    private final String perkId;
    private final int slots;
    private final int minReq;

    public ClanVaultSlot(String perkId, int slots, int minReq) {
        this.perkId = perkId;
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
        return new Component[]{
                Component.text("Gain " + slots + " extra slots for your clan vault.", NamedTextColor.GRAY)
        };
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder().material(Material.CHEST).build();
    }

    @Override
    public ClanPerkCategory getCategory() {
        return ClanPerkCategory.VAULT;
    }

}
