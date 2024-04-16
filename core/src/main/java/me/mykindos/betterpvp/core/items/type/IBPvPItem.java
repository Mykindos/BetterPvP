package me.mykindos.betterpvp.core.items.type;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface IBPvPItem {
    ItemStack getItemStack();
    ItemStack getItemStack(int count);
    ItemStack itemify(ItemStack itemStack);
    String getIdentifier();
    @NotNull Component getName();
    default List<Component> getLore(ItemMeta meta) {
        return new ArrayList<>();
    }
    String getSimpleName();
    default boolean isEnabled() {
        return true;
    }
    boolean matches(ItemStack itemStack);
    boolean compareExactItem(ItemStack itemStack1, ItemStack itemStack2);
    ShapedRecipe getShapedRecipe(String... shape);
    ShapedRecipe getShapedRecipe(int recipeNumber, String... shape);
    ShapedRecipe getShapedRecipe(int count, int recipeNumber, String... shape);
    ShapedRecipe getShapedRecipe(int count, String key_suffix, String... shape);
    ShapelessRecipe getShapelessRecipe(int count, String key_suffix, ItemStack... ingredients);
    void damageItem(Player player, ItemStack itemStack, int damage);
    ItemMeta applyLore(ItemStack itemStack, ItemMeta itemMeta);
}
