package me.mykindos.betterpvp.core.framework.hat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@FunctionalInterface
public interface HatProvider extends Function<Player, ItemStack> {

    @Override
    ItemStack apply(Player player);

}
