package me.mykindos.betterpvp.core.utilities.model.item;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder(toBuilder = true)
@Value
@Getter
public class ItemView implements ItemProvider {

    @Nullable ItemMeta baseMeta;
    @Nullable Component displayName;
    @NotNull Material material;
    @Nullable @Builder.Default @Range(from = 0, to = Integer.MAX_VALUE) Integer customModelData = 0;
    @Nullable Material fallbackMaterial;
    @Builder.Default @Range(from = 1, to = Integer.MAX_VALUE) int amount = 1;
    @Builder.Default @Range(from = -1, to = Integer.MAX_VALUE) Integer durability = 0;
    List<EnchantmentEntry> enchantments;
    /**
     * Creates lore for before the outline, if frameLore is true
     */
    @Singular("prelore") List<? extends Component> prelore;
    @Singular("lore") List<? extends Component> lore;
    @Singular List<ItemFlag> flags;
    @Builder.Default boolean frameLore = false;
    @Builder.Default boolean glow = false;
    @Singular Map<ClickAction, ? extends Component> actions;

    /**
     * Creates a new {@link ItemView} from an {@link ItemStack}.
     * @param itemStack the {@link ItemStack} to create the view from
     * @return the created {@link ItemView}
     */
    public static ItemView of(@NotNull ItemStack itemStack) {
        return ItemView.builder().with(itemStack).build();
    }

    public List<EnchantmentEntry> getEnchantments() {
        return Collections.unmodifiableList(enchantments);
    }

    public List<Component> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public Map<ClickAction, Component> getClickActions() {
        return Collections.unmodifiableMap(actions);
    }

    public List<ItemFlag> getFlags() {
        return Collections.unmodifiableList(flags);
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material, Math.max(1, amount));
        if (baseMeta != null) {
            itemStack.setItemMeta(baseMeta);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }

        if (durability != null && meta instanceof Damageable damageable) {
            damageable.setDamage(durability);
        }

        if (displayName != null) {
            meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        if (frameLore && meta.hasLore()) {
            List<Component> divided = Objects.requireNonNull(meta.lore());

            divided.add(0, UtilMessage.DIVIDER);
            divided.add(1, Component.empty());
            divided.add(Component.empty());
            if (!prelore.isEmpty()) {
                divided.addAll(0, prelore);
            }
            divided.add(UtilMessage.DIVIDER);
            meta.lore(divided);
        }

        meta.setAttributeModifiers(material.getDefaultAttributeModifiers());

        if (!actions.isEmpty()) {
            final List<Component> currentLore = Objects.requireNonNullElse(meta.lore(), new ArrayList<>());
            currentLore.add(Component.empty());
            actions.forEach((clickType, action) -> {
                final TextComponent clickTo = Component.text(clickType.getName() + " to ", NamedTextColor.WHITE);
                final TextComponent result = clickTo.append(action.applyFallbackStyle(NamedTextColor.YELLOW));
                currentLore.add(result);
            });
            meta.lore(currentLore);
        }

        final List<Component> curLore = meta.lore();
        if (curLore != null) {
            meta.lore(curLore.stream().map(component -> UtilMessage.normalize(component).decoration(TextDecoration.ITALIC, false)).toList());
        }

        if (enchantments != null) {
            for (EnchantmentEntry enchantment : enchantments) {
                meta.addEnchant(enchantment.enchantment(), enchantment.level(), true);
            }
        }

        if (customModelData != null) {

            if (Compatibility.ITEMS_ADDER || fallbackMaterial == null) {
                meta.setCustomModelData(customModelData);
            } else {
                meta.setCustomModelData(null);
                itemStack.setType(Objects.requireNonNullElse(fallbackMaterial, material));
            }
        }

        if (flags != null) {
            meta.addItemFlags(flags.toArray(ItemFlag[]::new));
        }

        if (glow) {
            UtilItem.addGlow(meta);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public ItemStack get(@Nullable String lang) {
        return get();
    }

    @Override
    public ItemStack get() {
        return toItemStack();
    }

    public SimpleItem toSimpleItem() {
        return new SimpleItem(this);
    }

    public record EnchantmentEntry(Enchantment enchantment, int level) { }

    public record Texture(int customModelData, Material material) { }

    public static class ItemViewBuilder {
        public ItemViewBuilder enchantment(@NonNull Enchantment enchantment, @Range(from = 0, to = Integer.MAX_VALUE) int level) {
            enchantments.add(new EnchantmentEntry(enchantment, level));
            return this;
        }

        public ItemViewBuilder with(@NotNull ItemStack itemStack) {
            final ItemMeta meta = itemStack.getItemMeta();
            this.baseMeta(meta);
            this.material(itemStack.getType());
            this.amount(itemStack.getAmount());

            final Component displayName = meta.displayName();
            if (displayName != null) {
                this.displayName(displayName);
            }

            final List<Component> lore = meta.lore();
            if (lore != null ) {
                this.lore(lore);
            }

            if (meta instanceof Damageable damageable) {
                this.durability(damageable.getDamage());
            }

            if (meta.hasCustomModelData()) {
                this.customModelData(meta.getCustomModelData());
            }

            if (meta.hasEnchants()) {
                for (Enchantment enchantment : meta.getEnchants().keySet()) {
                    this.enchantment(enchantment, meta.getEnchants().get(enchantment));
                }
            }

            meta.getItemFlags().forEach(this::flag);

            return this;
        }
    }

}
