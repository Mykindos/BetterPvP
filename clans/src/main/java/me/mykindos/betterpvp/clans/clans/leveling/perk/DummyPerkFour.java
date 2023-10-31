package me.mykindos.betterpvp.clans.clans.leveling.perk;

import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.utilities.model.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class DummyPerkFour implements ClanPerk {
    @Override
    public String getName() {
        return "Dummy Feature 2";
    }

    @Override
    public int getMinimumLevel() {
        return 30;
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                Component.text("This is a dummy perk numero dos!", NamedTextColor.GRAY)
        };
    }

    @Override
    public ItemStack getIcon() {
        return ItemView.builder().material(Material.BEACON).build().toItemStack();
    }
}
