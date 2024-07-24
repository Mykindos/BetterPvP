package me.mykindos.betterpvp.clans.clans.core;

import lombok.Getter;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.OptionalInt;

@Getter
public enum EnergyItem {

    SHARD("Energy Shard", Material.AMETHYST_SHARD),
    SMALL_CRYSTAL("Energy Crystal", Material.MEDIUM_AMETHYST_BUD),
    LARGE_CRYSTAL("Large Energy Crystal", Material.LARGE_AMETHYST_BUD),
    GIANT_CRYSTAL("Giant Energy Cluster", Material.AMETHYST_CLUSTER);

    private final String name;
    private final Material material;

    EnergyItem(String name, Material material){
        this.name = name;
        this.material = material;
    }

    public static OptionalInt getEnergyAmount(@NotNull ItemStack itemStack, boolean enforceAutoDeposit) {
        if (!itemStack.hasItemMeta()) {
            return OptionalInt.empty(); // Not an energy-shard
        }

        final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.ENERGY_AMOUNT, PersistentDataType.INTEGER)) {
            return OptionalInt.empty(); // Not an energy-shard
        }

        if (enforceAutoDeposit && !pdc.has(ClansNamespacedKeys.AUTO_DEPOSIT, PersistentDataType.BOOLEAN)) {
            return OptionalInt.empty(); // Not an auto-deposit energy-shard
        }

        final int energy = Objects.requireNonNullElse(pdc.get(ClansNamespacedKeys.ENERGY_AMOUNT, PersistentDataType.INTEGER), 0);
        return OptionalInt.of(energy * itemStack.getAmount());
    }

    public static @NotNull EnergyItem fromType(@NotNull Material material) {
        for (EnergyItem energyItem : values()) {
            if (energyItem.getMaterial().equals(material)) {
                return energyItem;
            }
        }

        throw new IllegalArgumentException("Material " + material + " is not an energy item.");
    }

    public ItemStack generateItem(int amount, boolean autoDeposit) {
        final ItemStack item = ItemView.builder()
                .material(material)
                .displayName(Component.text(this.name, TextColor.color(227, 156, 255)))
                .frameLore(true)
                .lore(Component.text("Deposit this item into your clan core", NamedTextColor.GRAY))
                .lore(Component.text("to gain energy.", NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Component.text("This item yields ", NamedTextColor.GRAY)
                        .append(Component.text(amount + " energy", NamedTextColor.YELLOW)))
                .build()
                .get();

        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ClansNamespacedKeys.ENERGY_AMOUNT, PersistentDataType.INTEGER, amount);
        if (autoDeposit) {
            pdc.set(ClansNamespacedKeys.AUTO_DEPOSIT, PersistentDataType.BOOLEAN, true);
        }

        item.setItemMeta(meta);
        return item;
    }

}
