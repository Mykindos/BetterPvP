package me.mykindos.betterpvp.core.menu.button.filter;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class RaritySearchButton extends AbstractItem implements CooldownButton {

    private final Supplier<ItemRarity> getter;
    private final Consumer<ItemRarity> setter;
    private final Runnable refresh;

    @Override
    public ItemProvider getItemProvider() {
        final ItemView.ItemViewBuilder builder = ItemView.builder();
        builder.material(Material.PAPER);
        builder.itemModel(Key.key("betterpvp", "menu/icon/regular/crown_icon"));

        final ItemRarity selected = getter.get();
        boolean titled = false;
        for (ItemRarity itemRarity : valuesWithAll()) {
            TextColor color = selected == itemRarity ? TextColor.color(itemRarity == null ? NamedTextColor.YELLOW : itemRarity.getColor()) : NamedTextColor.GRAY;
            String name = itemRarity == null ? "All Rarities" : itemRarity.getName();
            if (!titled) {
                builder.displayName(Component.text(name, color));
                titled = true;
            } else {
                builder.lore(Component.text(name, color));
            }
        }

        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final ItemRarity[] pool = valuesWithAll();
        int index = indexOf(pool, getter.get());

        if (clickType.isLeftClick()) {
            index = (index + 1) % pool.length;
        } else {
            index = (index - 1 + pool.length) % pool.length;
        }

        setter.accept(pool[index]);
        refresh.run();
        notifyWindows();
    }

    @Override
    public double getCooldown() {
        return 0.4;
    }

    private ItemRarity[] valuesWithAll() {
        final ItemRarity[] values = ItemRarity.values();
        final ItemRarity[] pool = new ItemRarity[values.length + 1];
        pool[0] = null;
        System.arraycopy(values, 0, pool, 1, values.length);
        return pool;
    }

    private int indexOf(ItemRarity[] pool, ItemRarity target) {
        for (int i = 0; i < pool.length; i++) {
            if (pool[i] == target) {
                return i;
            }
        }
        return 0;
    }
}
