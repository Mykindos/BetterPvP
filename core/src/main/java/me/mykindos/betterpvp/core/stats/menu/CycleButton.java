package me.mykindos.betterpvp.core.stats.menu;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CycleButton<T> extends AbstractItem implements CooldownButton {

    private final T[] pool;
    private int index;
    private final Component name;
    private final ItemStack itemStack;
    private final Consumer<T> callback;

    public CycleButton(T[] pool, ItemStack item, Component name, Consumer<T> callback) {
        this.callback = callback;
        Preconditions.checkArgument(pool.length > 0, "Pool must have at least one element");
        this.itemStack = item;
        this.pool = pool;
        this.name = name;
        this.index = 0;
    }

    private String getElementName(T element){
        if (element instanceof FilterType filterType){
            return filterType.getName();
        } else if (element instanceof SortType sortType){
            return sortType.getName();
        }
        return element.toString();
    }

    public void cycle(int amount){
        index += amount;
        if (index >= pool.length){
            index = 0;
        } else if (index < 0){
            index = pool.length - 1;
        }
    }

    public T getCurrent(){
        return pool[index];
    }

    @Override
    public ItemProvider getItemProvider() {
        List<Component> sortTypeLore = new ArrayList<>();
        for (T type : pool) {
            String elemName = getElementName(type);
            NamedTextColor color = NamedTextColor.GRAY;
            final boolean isSelected = pool[index] == type;
            if (isSelected) {
                elemName += " \u00AB";
                color = NamedTextColor.GREEN;
            }
            sortTypeLore.add(Component.text(elemName, color));
        }

        final ItemStack result = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(this.itemStack, this.name, sortTypeLore));
        return ItemView.of(result);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()){
            cycle(1);
        } else if (clickType.isRightClick()){
            cycle(-1);
        }

        callback.accept(getCurrent());
        notifyWindows();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public double getCooldown() {
        return 0.2;
    }
}
