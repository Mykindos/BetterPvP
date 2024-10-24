package me.mykindos.betterpvp.clans.clans.coins;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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
import org.jetbrains.annotations.Range;

import java.util.Objects;
import java.util.OptionalInt;

@Getter
public enum CoinItem {

    SMALL_NUGGET("Small Gold Nugget", Material.GOLD_NUGGET),
    LARGE_NUGGET("Large Gold Nugget", Material.RAW_GOLD),
    BAR("Gold Bar", Material.GOLD_INGOT);

    private final String name;
    private final Material material;

    CoinItem(String name, Material material){
        this.name = name;
        this.material = material;
    }

    public static OptionalInt getCoinAmount(@NotNull ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return OptionalInt.empty(); //Not a coin item
        }

        final PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.COIN_AMOUNT, PersistentDataType.INTEGER)) {
            return OptionalInt.empty(); //Not a coin item
        }

        final int coins = Objects.requireNonNullElse(pdc.get(ClansNamespacedKeys.COIN_AMOUNT, PersistentDataType.INTEGER), 0);
        return OptionalInt.of(coins);
    }

    public ItemStack generateItem(int amount) {
        Preconditions.checkArgument(amount > 0, "Amount must be greater than 0.");

        final ItemStack item = ItemView.builder()
                .material(material)
                .displayName(Component.text(this.name, TextColor.color(255, 215, 0)))
                .frameLore(true)
                .lore(Component.text("Contains ", NamedTextColor.GRAY)
                        .append(Component.text(UtilFormat.formatNumber(amount) + " coins.", NamedTextColor.YELLOW)))
                .build()
                .get();

        item.editMeta(meta -> {
            final PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ClansNamespacedKeys.COIN_AMOUNT, PersistentDataType.INTEGER, amount);
            pdc.set(ClansNamespacedKeys.AUTO_DEPOSIT, PersistentDataType.BOOLEAN, true);
        });

        return item;
    }

}

