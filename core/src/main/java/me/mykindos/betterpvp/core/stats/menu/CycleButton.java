package me.mykindos.betterpvp.core.stats.menu;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CycleButton<T> extends AbstractItem implements CooldownButton {

    private final T[] pool;
    private int index;
    private final Function<T, Component> name;
    private final Function<T, Material> material;
    private final Consumer<T> callback;

    public CycleButton(T[] pool, Function<T, Material> type, Function<T, Component> name, Consumer<T> callback) {
        this.callback = callback;
        Preconditions.checkArgument(pool.length > 0, "Pool must have at least one element");
        this.material = type;
        this.pool = pool;
        this.name = name;
        this.index = 0;
    }

    public CycleButton(T[] pool, ItemStack item, Function<T, Component> name, Consumer<T> callback) {
        this(pool, type -> item.getType(), name, callback);
    }

    public CycleButton(T[] pool, ItemStack item, Component name, Consumer<T> callback) {
        this(pool, item, type -> name, callback);
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

        final ItemStack result = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(
                new ItemStack(this.material.apply(getCurrent())),
                this.name.apply(getCurrent()),
                sortTypeLore));
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
