package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@EqualsAndHashCode(of = { "rarity" }, callSuper = false)
@ToString(of = { "rarity" })
public final class RarityRestriction extends VaultRestriction {

    @Getter
    private final ItemRarity rarity;
    private final ItemFactory itemFactory;

    public RarityRestriction(int allowedCount, ItemRarity clazz) {
        super(allowedCount);
        this.rarity = clazz;
        this.itemFactory = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemFactory.class);
    }

    public RarityRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks, ItemRarity rarity) {
        super(allowedPerks);
        this.rarity = rarity;
        this.itemFactory = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemFactory.class);
    }

    @Override
    public boolean matches(@NotNull ItemStack itemStack) {
        return itemFactory.fromItemStack(itemStack)
                .map(ItemInstance::getRarity)
                .filter(rarity -> rarity == this.rarity)
                .isPresent();
    }
}
