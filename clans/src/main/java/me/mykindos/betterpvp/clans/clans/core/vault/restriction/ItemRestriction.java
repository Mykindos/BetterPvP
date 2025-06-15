package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@EqualsAndHashCode(of = { "item" }, callSuper = false)
@ToString(of = { "item" })
public final class ItemRestriction extends VaultRestriction {

    @Getter
    private final BaseItem item;
    private final ItemFactory itemFactory;

    public ItemRestriction(int allowedCount, BaseItem clazz) {
        super(allowedCount);
        this.item = clazz;
        this.itemFactory = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemFactory.class);
    }

    public ItemRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks, BaseItem item) {
        super(allowedPerks);
        this.item = item;
        this.itemFactory = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemFactory.class);
    }

    @Override
    public boolean matches(@NotNull ItemStack itemStack) {
        return itemFactory.fromItemStack(itemStack)
                .map(instance -> instance.getBaseItem() == item)
                .orElse(false);
    }
}
