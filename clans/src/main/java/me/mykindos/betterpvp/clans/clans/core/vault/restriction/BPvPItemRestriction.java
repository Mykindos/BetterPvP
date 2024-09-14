package me.mykindos.betterpvp.clans.clans.core.vault.restriction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@EqualsAndHashCode(of = { "clazz" }, callSuper = false)
@ToString(of = { "clazz" })
public final class BPvPItemRestriction extends VaultRestriction {

    @Getter
    private final Class<?> clazz;
    private final ItemHandler itemHandler;

    public BPvPItemRestriction(int allowedCount, Class<?> clazz) {
        super(allowedCount);
        this.clazz = clazz;
        this.itemHandler = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemHandler.class);
    }

    public BPvPItemRestriction(@NotNull Map<@NotNull ClanPerk, @NotNull Integer> allowedPerks, Class<?> clazz) {
        super(allowedPerks);
        this.clazz = clazz;
        this.itemHandler = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ItemHandler.class);
    }

    @Override
    public boolean matches(@NotNull ItemStack itemStack) {
        return clazz.isInstance(itemHandler.getItem(itemStack));
    }
}
