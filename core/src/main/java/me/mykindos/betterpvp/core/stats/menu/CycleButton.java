package me.mykindos.betterpvp.core.stats.menu;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CycleButton<T> extends Button {

    private final T[] pool;
    private int index;
    private final IRefreshingMenu menu;

    public CycleButton(int slot, T[] pool, ItemStack item, Component name, Menu menu) {
        super(slot, item, name);
        Preconditions.checkArgument(menu instanceof IRefreshingMenu, "Menu must implement IRefreshingMenu");
        Preconditions.checkArgument(pool.length > 0, "Pool must have at least one element");
        this.pool = pool;
        this.index = 0;
        this.menu = ((IRefreshingMenu) menu);
        updateLore();
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
    public double getClickCooldown() {
        return 0.1;
    }

    private void updateLore() {
        List<Component> sortTypeLore = new ArrayList<>();
        for (T type : pool) {
            String name = getElementName(type);
            NamedTextColor color = NamedTextColor.GRAY;
            final boolean isSelected = pool[index] == type;
            if (isSelected) {
                name += " \u00AB";
                color = NamedTextColor.GREEN;
            }
            sortTypeLore.add(Component.text(name, color));
        }
        setLore(sortTypeLore);
        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(this.itemStack, this.name, this.lore));
        ((Menu) this.menu).refreshButton(this);
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()){
            cycle(1);
        } else if (clickType.isRightClick()){
            cycle(-1);
        }
        updateLore();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 2f);
    }
}
