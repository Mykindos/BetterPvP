package me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Singleton
@EqualsAndHashCode
public class UnbreakingRune implements Rune {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "unbreaking");

    @Inject
    private UnbreakingRune() {
    }

    @Override
    public @NotNull String getDescription() {
        return "Prevents items from losing durability when used.";
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Unbreaking";
    }

    @Override
    public boolean canApply(@NotNull Item item) {
        return (item instanceof ItemInstance instance && isDamageable(instance.createItemStack()))
                || (item instanceof BaseItem base && isDamageable(base.getModel()));
    }

    private boolean isDamageable(@NotNull ItemStack itemStack) {
        return itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof Damageable;
    }
}
