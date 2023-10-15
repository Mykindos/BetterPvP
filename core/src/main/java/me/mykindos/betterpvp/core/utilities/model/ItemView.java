package me.mykindos.betterpvp.core.utilities.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Objects;

@Builder
public class ItemView {

    private @Nullable Component displayName;
    private @NotNull Material material;
    private @Nullable @Builder.Default @Range(from = 0, to = Integer.MAX_VALUE) Integer customModelData = 0;
    private @Nullable Material fallbackMaterial;
    private @Builder.Default @Range(from = 1, to = Integer.MAX_VALUE) int amount = 1;
    private @Builder.Default @Range(from = -1, to = Integer.MAX_VALUE) Integer durability = 0;
    private List<EnchantmentEntry> enchantments;
    private @Singular("loreLine") List<? extends Component> lore;

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material, amount);
        final ItemMeta meta = itemStack.getItemMeta();
        if (durability != null && meta instanceof Damageable damageable) {
            damageable.setDamage(durability);
        }

        meta.displayName(displayName);
        meta.lore(lore);
        if (enchantments != null) {
            for (EnchantmentEntry enchantment : enchantments) {
                meta.addEnchant(enchantment.enchantment(), enchantment.level(), true);
            }
        }

        if (customModelData != null) {
            if (Compatibility.ITEMS_ADDER) {
                meta.setCustomModelData(customModelData);
            } else {
                meta.setCustomModelData(null);
                itemStack.setType(Objects.requireNonNullElse(fallbackMaterial, material));
            }
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public record EnchantmentEntry(Enchantment enchantment, int level) { }

    public record Texture(int customModelData, Material material) { }

    public static class ItemViewBuilder {
        public ItemViewBuilder enchantment(@NonNull Enchantment enchantment, @Range(from = 0, to = Integer.MAX_VALUE) int level) {
            enchantments.add(new EnchantmentEntry(enchantment, level));
            return this;
        }
    }

}
